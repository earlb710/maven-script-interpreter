package com.eb.ui.ebs;

import com.eb.script.RuntimeContext;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.file.FileContext;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.builtins.Builtins;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.json.Json;
import com.eb.script.parser.ParseError;
import com.eb.ui.cli.ScriptArea;
import com.eb.ui.tabs.TabContext;
import com.eb.util.Util;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.prefs.Preferences;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 * @author Earl Bosch
 */
public class EbsConsoleHandler extends EbsHandler {

    protected static final int RECENT_MAX = 10;
    protected static final String PREF_NODE = "com.eb.ui.cli.EbsApp.recent";
    protected static final String PREF_KEY_PREFIX = "recentFile.";
    
    /** Maximum length for error message display before truncation */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 60;
    /** Length to truncate error message to (with room for "...") */
    private static final int TRUNCATED_MESSAGE_LENGTH = 57;
    
    /** 
     * Cached resources directory path for application screens.
     * Lazily initialized on first use.
     */
    private static Path cachedResourcesDir = null;

    protected final Stage stage;
    protected final Deque<Path> recentFiles = new ArrayDeque<>(); // Most recent at the head
    private int newScriptSequence = 1; // Sequence number for new script files
    private ProjectTreeView projectTreeView; // Reference to the project tree view

    public EbsConsoleHandler(Stage stage, RuntimeContext ctx) {
        super(ctx);
        this.stage = stage;
    }
    
    /**
     * Set the project tree view reference.
     * @param projectTreeView The project tree view component
     */
    public void setProjectTreeView(ProjectTreeView projectTreeView) {
        this.projectTreeView = projectTreeView;
    }

    /**
     * Process a submitted lines
     */
    @Override
    public void submit(String... lines) throws IOException, ParseError, InterpreterError {
        ScriptArea output = env.getOutputArea();
        for (String line : lines) {
            // Handle simple commands
            if (line != null && !line.isBlank()) {
                line = line.trim();
                // Check for console commands (start with single '/') but not EBS comments (start with '//')
                if (line.charAt(0) == '/' && (line.length() < 2 || line.charAt(1) != '/')) {
                    String cmd = line.toLowerCase();
                    if (cmd.startsWith("/list ")) {
                        line = line.substring(6).trim();
                        cmd = line.toLowerCase();
                        ArrayDynamic arrayResult = null;
                        if (cmd.length() >= 5 && cmd.substring(0, 5).equals("files")) {
                            if (cmd.length() > 5) {
                                line = line.substring(6).trim();
                            } else {
                                line = ".";
                            }
                            arrayResult = (ArrayDynamic) Builtins.callBuiltin(env, "file.listfiles", line);
                            if (arrayResult.isEmpty()) {
                                output.println("No files.");
                            } else {
                                output.println("List files : " + arrayResult.size());
                                output.println(Json.prettyJson(arrayResult));
                            }
                            continue;
                        } else if (cmd.equals("open files") || cmd.equals("openfiles")) {
                            arrayResult = (ArrayDynamic) Builtins.callBuiltin(env, "file.openfiles");
                        }
                        // pretty‑print your JSON‑like structure
                        if (arrayResult != null) {
                            if (arrayResult.isEmpty()) {
                                output.println("No open files.");
                            } else {
                                output.println("Open files : " + arrayResult.size());
                                for (Object o : arrayResult) {
                                    Map m = (Map) o;
                                    output.println(
                                            m.get("handle") + "  "
                                            + m.get("mode") + "  "
                                            + m.get("size") + "B  "
                                            + m.get("path")
                                    );
                                }
                            }
                        } else {
                            output.println(String.valueOf(arrayResult));
                        }
                        continue;

                    } else if (cmd.startsWith("/open")) {
                        // Use the quote-aware splitter you added earlier
                        String[] parts = Util.splitArgsAllowingQuotes(line.substring(line.indexOf(' ')).trim());

                        String path = (parts.length >= 1 ? parts[0] : null);
                        String mode = (parts.length >= 2 ? parts[1] : "rw");  // default read
                        String enc = (parts.length >= 3 ? parts[2] : null); // optional encoding for display

                        String handle = (String) Builtins.callBuiltin(env, "file.open", path, mode);
                        output.printlnOk("opened: " + handle);

                        // Create viewer tab using the path (not the handle)
                        if (path != null) {
                            Path p = Util.resolveSandboxedPath(path);
                            FileContext ofile = new FileContext(handle, p, mode);
                            tabHandler.showTab(new TabContext(p.getFileName().toString(), p, ofile), true);
                        }
                        continue;
                    } else if (line.startsWith("/close ")) {
                        String[] parts = Util.splitArgsAllowingQuotes(line.substring(line.indexOf(' ')).trim());
                        for (String key : parts) {
                            Boolean ok = (Boolean) Builtins.callBuiltin(env, "file.close", key);
                            if (env.isEchoOn()) {
                                if (ok) {
                                    output.printlnOk("closed : " + key);
                                } else {
                                    output.printlnWarn("Could not close : " + key);
                                }
                            }
                        }
                        continue;
                    } else if (cmd.startsWith("/echo")) {
                        handleEcho(output, cmd);
                        continue;
                    } else if (cmd.startsWith("/debug")) {
                        handleDebug(output, cmd);
                        continue;
                    } else if (cmd.startsWith("/package")) {
                        handlePackage(output, line);
                        continue;
                    } else {
                        // Check if /help has a parameter
                        if (cmd.startsWith("/help ") || cmd.startsWith("/?")) {
                            String param = "";
                            if (cmd.startsWith("/help ") && cmd.length() > 6) {
                                param = line.substring(6).trim();
                            } else if (cmd.startsWith("/? ") && cmd.length() > 3) {
                                param = line.substring(3).trim();
                            }
                            
                            if (!param.isEmpty()) {
                                if (param.equalsIgnoreCase("keywords")) {
                                    Builtins.callBuiltin(env, "system.help");
                                } else {
                                    // Display help for specific keyword or builtin
                                    displayDetailedHelp(output, param);
                                }
                                continue;
                            }
                        }
                        
                        switch (cmd) {
                            case "/?":
                            case "/help":
                                output.println("Commands:");
                                output.println("  <b>control + enter</b>  <i>- submit input</i>");
                                output.println("  <b>control + up</b>     <i>- previous input from history</i>");
                                output.println("  <b>control + down</b>   <i>- next input from history</i>");
                                output.println("  <b>/help or /?</b>      <i>- show this help</i>");
                                output.println("  <b>/help keywords</b>   <i>- show list of keywords and builtins</i>");
                                output.println("  <b>/help &lt;keyword|builtin&gt;</b> <i>- show detailed help for a keyword or builtin (e.g., /help file.readTextFile)</i>");
                                output.println("  <b>/clear</b>           <i>- clear the console</i>");
                                output.println("  <b>/reset</b>           <i>- clear all variables</i>");
                                output.println("  <b>/time</b>            <i>- print the current time</i>");
                                output.println("  <b>/open filename</b>   <i>- open file returns fileHandle</i>");
                                output.println("  <b>/close fileHandle</b><i>- close</i>");
                                output.println("  <b>/list files [dir]</b><i>- list files in current directory or [directroy]</i>");
                                output.println("  <b>/list open files</b> <i>- list open filenames</i>");
                                output.println("  <b>/echo  [on|off]</b>  <i>- toggle interpreter echo flag (current: " + (env.isEchoOn() ? "on" : "off") + ")");
                                output.println("  <b>/debug [on|off|trace on|trace off]</b> <i>- toggle interpreter debug/trace flags");
                                output.println("  <b>/package &lt;input.ebs&gt; [output.ebsp]</b> <i>- package script to binary .ebsp file");
                                output.println("  <b>/exit</b>            <i>- close console</i>");
                                continue;

                            case "/help keywords":
                                Builtins.callBuiltin(env, "system.help");
                                continue;
                            case "/exit":
                                exit();
                                break;
                            case "/clear":
                                output.clear();
                                continue;

                            case "/reset":
                                env.clear();
                                if (env.isEchoOn()) {
                                    output.printlnOk("> globals cleared.\n");
                                }
                                continue;

                            case "/time":
                                output.printlnOk("Time: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                                continue;

                            default:
                        }
                    }
                } else {
                    if (line.length() > 0) {
                        super.submit(line);
                    }
                }
            }
        }
    }

    private void handleEcho(ScriptArea output, String cmd) {
        String[] parts = cmd.split("\\s+");
        if (parts.length == 1) {
            output.printlnInfo("echo is currently " + (env.isEchoOn() ? "on" : "off") + "  (usage: /echo on|off)");
            return;
        }
        if ("on".equalsIgnoreCase(parts[1])) {
            env.setEcho(true);   // interpreter reads this flag from Environment
            output.printlnOk("echo -> on");
        } else if ("off".equalsIgnoreCase(parts[1])) {
            env.setEcho(false);
            output.printlnOk("echo -> off");
        } else {
            output.printlnWarn("usage: /echo on|off");
        }
    }

    private void handleDebug(ScriptArea output, String cmd) {
        String[] parts = cmd.split("\\s+");
        if (parts.length == 1) {
            output.printlnInfo("debug: " + (dbg.isDebugOn() ? "on" : "off") + ", trace: " + (dbg.isDebugTraceOn() ? "on" : "off"));
            output.printlnInfo("usage: /debug on|off | /debug trace on|off");
            return;
        }
        if ("on".equalsIgnoreCase(parts[1])) {
            dbg.setDebugOn();    // toggles Debugger.DEBUG_ENABLED
            output.printlnOk("debug -> on");
            return;
        }
        if ("off".equalsIgnoreCase(parts[1])) {
            dbg.setDebugOff();
            output.printlnOk("debug -> off");
            return;
        }
        if ("trace".equalsIgnoreCase(parts[1]) && parts.length >= 3) {
            if ("on".equalsIgnoreCase(parts[2])) {
                dbg.setDebugTraceOn();  // interpreter wraps statements when trace is on
                output.printlnOk("debug trace -> on");
            } else if ("off".equalsIgnoreCase(parts[2])) {
                dbg.setDebugTraceOff();
                output.printlnOk("debug trace -> off");
            } else {
                output.printlnWarn("usage: /debug trace on|off");
            }
            return;
        }
        output.printlnWarn("usage: /debug on|off | /debug trace on|off");
    }

    private void handlePackage(ScriptArea output, String line) {
        try {
            String[] parts = Util.splitArgsAllowingQuotes(line.substring(line.indexOf(' ')).trim());
            
            if (parts.length == 0) {
                output.printlnWarn("usage: /package <input.ebs> [output.ebsp]");
                return;
            }
            
            String inputFile = parts[0];
            String outputFile = parts.length > 1 ? parts[1] : inputFile.replaceAll("(?i)\\.ebs$", "") + ".ebsp";
            
            Path inputPath = Util.resolveSandboxedPath(inputFile);
            Path outputPath = Util.resolveSandboxedPath(outputFile);
            
            if (!Files.exists(inputPath)) {
                output.printlnError("Input file not found: " + inputFile);
                return;
            }
            
            output.printlnInfo("Parsing: " + inputFile);
            RuntimeContext context = com.eb.script.parser.Parser.parse(inputPath);
            
            output.printlnInfo("Packaging to: " + outputFile);
            com.eb.script.package_tool.RuntimeContextSerializer.serialize(context, outputPath);
            
            long originalSize = Files.size(inputPath);
            long packagedSize = Files.size(outputPath);
            output.printlnOk("Package created successfully!");
            output.println("  Original size: " + originalSize + " bytes");
            output.println("  Packaged size: " + packagedSize + " bytes");
            
            if (packagedSize < originalSize) {
                output.println("  Size reduction: " + String.format("%.1f%%", 
                    (1.0 - (double)packagedSize / originalSize) * 100));
            } else {
                output.println("  Size increase: " + String.format("%.1f%%", 
                    ((double)packagedSize / originalSize - 1.0) * 100));
            }
            
        } catch (Exception e) {
            output.printlnError("Error packaging script: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayDetailedHelp(ScriptArea output, String itemName) {
        try {
            // Load help-lookup.json from resources
            InputStream is = getClass().getClassLoader().getResourceAsStream("help-lookup.json");
            if (is == null) {
                output.printlnWarn("Help system not available (help-lookup.json not found)");
                return;
            }
            
            String jsonContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            is.close();
            
            // Parse JSON
            Map<String, Object> lookup = (Map<String, Object>) Json.parse(jsonContent);
            
            // Search in keywords first
            ArrayDynamic keywords = (ArrayDynamic) lookup.get("keywords");
            if (keywords != null) {
                for (Object keywordObj : keywords) {
                    Map<String, Object> keyword = (Map<String, Object>) keywordObj;
                    String kwName = (String) keyword.get("keyword");
                    if (kwName != null && kwName.equalsIgnoreCase(itemName)) {
                        displayHelpEntry(output, kwName, keyword, "Keyword");
                        return;
                    }
                }
            }
            
            // Search in builtins
            ArrayDynamic builtins = (ArrayDynamic) lookup.get("builtins");
            if (builtins != null) {
                for (Object builtinObj : builtins) {
                    Map<String, Object> builtin = (Map<String, Object>) builtinObj;
                    String funcName = (String) builtin.get("function");
                    if (funcName != null && funcName.equalsIgnoreCase(itemName)) {
                        displayHelpEntry(output, funcName, builtin, "Builtin Function");
                        return;
                    }
                }
            }
            
            // Search in console commands
            ArrayDynamic consoleCommands = (ArrayDynamic) lookup.get("console_commands");
            if (consoleCommands != null) {
                for (Object cmdObj : consoleCommands) {
                    Map<String, Object> command = (Map<String, Object>) cmdObj;
                    String cmdName = (String) command.get("command");
                    if (cmdName != null && cmdName.equalsIgnoreCase(itemName)) {
                        displayHelpEntry(output, cmdName, command, "Console Command");
                        return;
                    }
                }
            }
            
            // Not found
            output.printlnWarn("No help found for: " + itemName);
            output.println("Use <b>/help keywords</b> to see all available keywords and builtins.");
            
        } catch (Exception ex) {
            output.printlnWarn("Error loading help: " + ex.getMessage());
        }
    }
    
    private void displayHelpEntry(ScriptArea output, String name, Map<String, Object> entry, String type) {
        output.println("<b>═══════════════════════════════════════════════════════════</b>");
        output.println("<b>" + type + ":</b> <u>" + name + "</u>");
        output.println("<b>═══════════════════════════════════════════════════════════</b>");
        
        String shortDesc = (String) entry.get("short_description");
        if (shortDesc != null && !shortDesc.isEmpty()) {
            output.println("<b>Description:</b> " + shortDesc);
            output.println("");
        }
        
        // For builtins, show parameters and return type
        if (entry.containsKey("parameters")) {
            ArrayDynamic params = (ArrayDynamic) entry.get("parameters");
            if (params != null && !params.isEmpty()) {
                // Convert ArrayDynamic to comma-separated string with name:type format
                StringBuilder paramStr = new StringBuilder();
                boolean first = true;
                for (Object param : params) {
                    if (!first) {
                        paramStr.append(", ");
                    }
                    // Handle both old string format and new map format
                    if (param instanceof Map) {
                        Map<String, Object> paramMap = (Map<String, Object>) param;
                        String paramName = (String) paramMap.get("name");
                        String paramType = (String) paramMap.get("type");
                        Boolean optional = (Boolean) paramMap.get("optional");
                        if (paramName != null) {
                            paramStr.append(paramName);
                            if (paramType != null && !paramType.isEmpty()) {
                                paramStr.append(":").append(paramType);
                            }
                            if (Boolean.TRUE.equals(optional)) {
                                paramStr.append("?");
                            }
                        }
                    } else {
                        // Fallback for old string format
                        paramStr.append(param.toString());
                    }
                    first = false;
                }
                output.println("<b>Parameters:</b> " + paramStr.toString());
            }
        }
        
        if (entry.containsKey("return_type")) {
            String returnType = (String) entry.get("return_type");
            if (returnType != null && !returnType.isEmpty()) {
                output.println("<b>Returns:</b> " + returnType);
            }
        }
        
        if (entry.containsKey("parameters") || entry.containsKey("return_type")) {
            output.println("");
        }
        
        String longHelp = (String) entry.get("long_help");
        if (longHelp != null && !longHelp.isEmpty()) {
            output.println("<b>Details:</b>");
            output.println(longHelp);
            output.println("");
        }
        
        String example = (String) entry.get("example");
        if (example != null && !example.isEmpty()) {
            output.println("<b>Example:</b>");
            output.println("<i>" + example + "</i>");
            output.println("");
        }
        
        String documentation = (String) entry.get("documentation");
        if (documentation != null && !documentation.isEmpty()) {
            output.println("<b>Additional Documentation:</b>");
            output.println(documentation);
        }
        
        output.println("<b>═══════════════════════════════════════════════════════════</b>");
    }

    @Override
    public void submitErrors(String... lines) {
        super.submitErrors(lines);
        // Update status bar with last error message
        if (statusBar != null && lines != null && lines.length > 0) {
            String lastError = lines[lines.length - 1];
            if (lastError != null && !lastError.isEmpty()) {
                // Truncate message if too long for display
                String displayMsg = lastError.length() > MAX_ERROR_MESSAGE_LENGTH 
                    ? lastError.substring(0, TRUNCATED_MESSAGE_LENGTH) + "..." 
                    : lastError;
                statusBar.setMessage(displayMsg, lastError); // full message in tooltip
            }
        }
    }

    public void addRecentFile(Path p) {
        if (p == null) {
            return;
        }
        try {
            p = p.toAbsolutePath().normalize();
        } catch (Exception e) {
            System.err.println("Warning: Could not normalize path: " + e.getMessage());
        }

        final Path pf = p;
        // Remove if already present, then add to front
        recentFiles.removeIf(x -> x.toAbsolutePath().normalize().equals(pf));
        recentFiles.addFirst(pf);

        // Trim to capacity
        while (recentFiles.size() > RECENT_MAX) {
            recentFiles.removeLast();
        }

        // Persist & refresh menu
        saveRecentFiles();
    }

    public void openRecent(Path p) {
        try {
            if (p == null) {
                return;
            }
            Path abs = p.toAbsolutePath().normalize();
            if (!Files.exists(abs)) {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                        "File not found:\n" + abs + "\n\nRemove it from Recent files?",
                        ButtonType.OK, ButtonType.CANCEL);
                a.setHeaderText("Missing file");
                a.initOwner(stage);
                a.initModality(Modality.APPLICATION_MODAL);
                var res = a.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    recentFiles.removeIf(x -> x.toAbsolutePath().normalize().equals(abs));
                    saveRecentFiles();
                }
                return;
            }
            // Open via builtin (same path as the “Open file…” action)
            submit("/open \"" + abs.toString().replace("\\", "\\\\") + "\" rw");

            // Bump it to the front as most-recent (in case it wasn’t there)
            addRecentFile(abs);
        } catch (Exception ex) {
            submitErrors("Open recent failed: " + ex.getMessage());
        }
    }

    public Deque<Path> getRecentFiles() {
        return recentFiles;
    }

    // ---- Preferences persistence ----
    public void loadRecentFiles() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            recentFiles.clear();
            for (int i = 0; i < RECENT_MAX; i++) {
                String v = prefs.get(PREF_KEY_PREFIX + i, null);
                if (v != null && !v.isBlank()) {
                    recentFiles.add(Path.of(v));
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load recent files from preferences: " + e.getMessage());
        }
    }

    public void saveRecentFiles() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            // Clear existing
            for (int i = 0; i < RECENT_MAX; i++) {
                prefs.remove(PREF_KEY_PREFIX + i);
            }
            // Store current order (0 = most recent)
            int i = 0;
            for (Path p : recentFiles) {
                if (i >= RECENT_MAX) {
                    break;
                }
                prefs.put(PREF_KEY_PREFIX + i, p.toString());
                i++;
            }
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Warning: Could not save recent files to preferences: " + e.getMessage());
        }
    }

    // EbsApp.java (inside class)
    // Save to given path via builtin file.writeFile(path, content)
    public void saveHandle(EbsTab tab) {
        try {
            TabContext contex = (TabContext) tab.getUserData();
            FileContext fileContext = contex.fileContext;
            if (!fileContext.closed) {
                callBuiltin("file.close", fileContext.handle);
            }
            callBuiltin("file.writeTextFile", contex.path.toString(), tab.getEditorText());
            tab.markCleanTitle();
        } catch (Exception ex) {
            submitErrors("Save failed: " + ex.getMessage());
        }
    }

    public void chooseOpenFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open a file");
        fc.setInitialDirectory(Util.SANDBOX_ROOT.toFile());
        //  filter
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("EBS Scripts", "*.ebs"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSS files", "*.css"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML files", "*.html"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown files", "*.md"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log files", "*.log"));
        var f = fc.showOpenDialog(stage);
        if (f != null) {
            try {
                // Add to MRU first (so even if open fails, you still see it; you can remove later if missing)
                addRecentFile(f.toPath());
                // Use builtin "file.open" through the console handler (keeps tab opening logic centralized)
                submit("/open \"" + f.getAbsolutePath().replace("\\", "\\\\") + "\" rw");
            } catch (Exception ex) {
                submitErrors("Failed to open file: " + ex.getMessage());
            }
        }
    }

    // Save As… with FileChooser, defaulting to sandbox path
    public void chooseSaveAs(EbsTab tab) {
        try {
            TabContext contex = (TabContext) tab.getUserData();
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Save As…");
            // Use your sandbox as the default folder:
            // Optional: suggest current file name if known
            if (contex != null) {
                if (contex.path != null) {
                    // Check if parent directory exists and is valid
                    Path parentDir = contex.path.getParent();
                    if (parentDir != null && Files.isDirectory(parentDir)) {
                        fc.setInitialDirectory(parentDir.toFile());
                    } else {
                        // Fall back to sandbox root
                        fc.setInitialDirectory(Util.SANDBOX_ROOT.toFile());
                    }
                    if (contex.path.getFileName() != null) {
                        fc.setInitialFileName(contex.path.getFileName().toString());
                    }
                } else {
                    fc.setInitialDirectory(Util.SANDBOX_ROOT.toFile());
                }

                File out = fc.showSaveDialog(stage);
                if (out != null) {
                    // Create a new TabContext with the updated path
                    Path newPath = out.toPath();
                    FileContext fileContext = new FileContext(null, newPath, "rw");
                    TabContext newContext = new TabContext(
                        newPath.getFileName() != null ? newPath.getFileName().toString() : newPath.toString(),
                        newPath,
                        fileContext
                    );
                    
                    // Update the tab's user data
                    tab.setUserData(newContext);
                    
                    // Write the file to the selected path
                    callBuiltin("file.writeTextFile", newPath.toString(), tab.getEditorText());
                    
                    // Add to recent files
                    addRecentFile(newPath);
                    
                    // Mark the tab as clean since we just saved
                    tab.markCleanTitle();
                    
                    // Update tab name to reflect the new filename
                    if (newPath.getFileName() != null) {
                        tab.setText(newPath.getFileName().toString());
                    }
                }
            }
        } catch (Exception ex) {
            submitErrors("Save as failed: " + ex.getMessage());
        }
    }

    public EventHandler<ActionEvent> fileChooser() {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                FileChooser fc = new FileChooser();
                fc.setTitle("Open a file");
                fc.setInitialDirectory(Util.SANDBOX_ROOT.toFile());
                // Optional: filter
                // fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("EBS Scripts", "*.ebs"));

                var f = fc.showOpenDialog(stage);
                if (f != null) {
                    try {
                        // Add to MRU first (so even if open fails, you still see it; you can remove later if missing)
                        addRecentFile(f.toPath());

                        // Use builtin "file.open" through the console handler (keeps tab opening logic centralized)
                        submit("/open \"" + f.getAbsolutePath().replace("\\", "\\\\") + "\" rw");
                    } catch (Exception ex) {
                        submitErrors("Failed to open file: " + ex.getMessage());
                    }
                }
            }
        };
    }

    public void exit() {
        javafx.application.Platform.exit();
    }

    /**
     * Generate the next available filename for a new script file.
     * Format: newScript_x.ebs where x is the sequence number.
     * Checks if file exists in sandbox and increments until an available name is found.
     */
    private String getNextNewScriptFilename() {
        String filename;
        Path path;
        do {
            filename = "newScript_" + newScriptSequence + ".ebs";
            path = Util.SANDBOX_ROOT.resolve(filename);
            newScriptSequence++;
        } while (Files.exists(path));
        return filename;
    }

    /**
     * Create a new file using the NewFileDialog.
     * Shows a dialog to select file type and name, with smart defaults.
     * The file is NOT created on disk - it exists only in the tab until saved.
     */
    public void createNewScriptFile() {
        try {
            // Show new file dialog with SANDBOX_ROOT as default path
            NewFileDialog dialog = new NewFileDialog(stage, Util.SANDBOX_ROOT.toString());
            var result = dialog.showAndWait();
            
            if (result.isEmpty()) {
                return; // User cancelled
            }
            
            NewFileDialog.FileInfo fileInfo = result.get();
            String fullPath = fileInfo.getFullPath();
            Path path = Path.of(fullPath);
            
            // DO NOT create the physical file - just create the tab
            // Create a TabContext with a null fileContext since the file doesn't exist yet
            TabContext tabContext = new TabContext(path.getFileName().toString(), path, null);
            
            // Create the tab using the tab handler
            EbsTab newTab = tabHandler.createNewTab(tabContext, true);
            
            if (newTab != null) {
                // Initialize with default content based on file type
                String defaultContent = getDefaultContentForFileType(fileInfo.getType());
                newTab.initializeAsNewFile(defaultContent);
            }
        } catch (Exception ex) {
            submitErrors("Failed to create new file: " + ex.getMessage());
        }
    }

    /**
     * Show a confirmation dialog when closing a tab with unsaved changes.
     * Offers options to Save, Save As, Don't Save, or Cancel.
     * @param tab The tab to potentially close
     * @return true if the tab should be closed, false if the close should be cancelled
     */
    public boolean confirmCloseTab(EbsTab tab) {
        if (tab == null || !tab.isDirty()) {
            return true; // No unsaved changes, close is OK
        }

        // Create custom button types
        ButtonType saveButton = new ButtonType("Save");
        ButtonType saveAsButton = new ButtonType("Save As...");
        ButtonType dontSaveButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Do you want to save changes?");
        alert.setContentText("The file \"" + tab.getText().replace("*", "") + "\" has unsaved changes.");
        alert.getButtonTypes().setAll(saveButton, saveAsButton, dontSaveButton, cancelButton);
        alert.initOwner(stage);
        alert.initModality(Modality.APPLICATION_MODAL);

        var result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == saveButton) {
                // Save the file to its default path (no dialog)
                TabContext context = (TabContext) tab.getUserData();
                if (context != null && context.path != null) {
                    try {
                        // Save directly to the path without showing dialog
                        callBuiltin("file.writeTextFile", context.path.toString(), tab.getEditorText());
                        tab.markCleanTitle();
                        addRecentFile(context.path);
                    } catch (Exception ex) {
                        submitErrors("Save failed: " + ex.getMessage());
                        return false; // Don't close if save failed
                    }
                }
                return true; // Close after saving
            } else if (result.get() == saveAsButton) {
                // Save As - show file chooser dialog
                chooseSaveAs(tab);
                return true; // Close after saving
            } else if (result.get() == dontSaveButton) {
                // Don't save, just close
                // Mark tab as clean so the close listener doesn't trigger again
                tab.markCleanTitle();
                return true;
            } else {
                // Cancel was pressed
                return false;
            }
        }

        // Dialog was closed without a selection, treat as cancel
        return false;
    }
    
    /**
     * Gets the resources directory path, calculating it once and caching for reuse.
     * Attempts multiple strategies to locate the resources directory:
     * 1. Use the current working directory structure (for development)
     * 2. Use the class location (for packaged JAR)
     * 
     * @return Path to resources directory, or null if not found
     */
    private static synchronized Path getResourcesDirectory() {
        if (cachedResourcesDir != null) {
            return cachedResourcesDir;
        }
        
        // Strategy 1: Try standard development structure (user.dir/ScriptInterpreter/src/main/resources)
        Path devPath = Path.of(System.getProperty("user.dir"), "ScriptInterpreter", "src", "main", "resources")
                .toAbsolutePath().normalize();
        if (Files.exists(devPath) && Files.isDirectory(devPath)) {
            cachedResourcesDir = devPath;
            return cachedResourcesDir;
        }
        
        // Strategy 2: Try from user.dir/resources (alternative structure)
        Path altPath = Path.of(System.getProperty("user.dir"), "resources")
                .toAbsolutePath().normalize();
        if (Files.exists(altPath) && Files.isDirectory(altPath)) {
            cachedResourcesDir = altPath;
            return cachedResourcesDir;
        }
        
        // Strategy 3: Try to infer from class location (for packaged JAR)
        try {
            java.net.URL resourceUrl = EbsConsoleHandler.class.getResource("/help-lookup.json");
            if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
                Path resourceFile = Path.of(resourceUrl.toURI());
                Path resourceDir = resourceFile.getParent();
                if (resourceDir != null && Files.exists(resourceDir) && Files.isDirectory(resourceDir)) {
                    cachedResourcesDir = resourceDir;
                    return cachedResourcesDir;
                }
            }
        } catch (Exception e) {
            // Ignore, will return null below
        }
        
        // Unable to locate resources directory
        return null;
    }
    
    /**
     * Run a script from a resource path. This method is designed to be called from menu actions
     * and other places that need to execute EBS scripts. It runs the script in a background
     * thread to avoid blocking the UI.
     * 
     * For application screens called from menus, this method temporarily sets the resources
     * directory as a safe directory so scripts can access resources like help-lookup.json.
     * 
     * @param resourcePath The path to the script resource (e.g., "/scripts/config_changes.ebs")
     * @param scriptName A friendly name for the script (used in log messages)
     */
    public void runScriptFromResource(String resourcePath, String scriptName) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                ScriptArea output = env.getOutputArea();
                javafx.application.Platform.runLater(() -> {
                    output.printlnError("ERROR: Could not find script resource: " + resourcePath);
                });
                return;
            }
            
            String script = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            // Update status bar
            if (statusBar != null) {
                javafx.application.Platform.runLater(() -> {
                    statusBar.setStatus("Running " + scriptName);
                    statusBar.clearMessage();
                });
            }
            
            // Set the resources directory as safe for application screens
            // This allows scripts to access resources like help-lookup.json
            Path resourcesDir = getResourcesDirectory();
            
            // Execute script in background thread like the Run button does
            Thread t = new Thread(() -> {
                // Set the resources directory as the context source directory for this script execution
                // Only set if we successfully located the resources directory
                if (resourcesDir != null) {
                    Util.setCurrentContextSourceDir(resourcesDir);
                }
                try {
                    // Submit script for execution
                    submit(script);
                    
                    // Update status bar on completion
                    javafx.application.Platform.runLater(() -> {
                        if (statusBar != null) {
                            statusBar.clearStatus();
                            statusBar.setMessage(scriptName + " completed");
                        }
                    });
                } catch (Exception ex) {
                    // Error message
                    ScriptArea output = env.getOutputArea();
                    javafx.application.Platform.runLater(() -> {
                        output.printlnError("✗ Error running " + scriptName + ": " + Util.formatExceptionWith2Origin(ex));
                        if (statusBar != null) {
                            statusBar.clearStatus();
                            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                            String displayMsg = errorMsg.length() > MAX_ERROR_MESSAGE_LENGTH 
                                ? errorMsg.substring(0, TRUNCATED_MESSAGE_LENGTH) + "..." 
                                : errorMsg;
                            statusBar.setMessage(displayMsg, errorMsg);
                        }
                    });
                } finally {
                    // Clear the context source directory after execution
                    // Only clear if we set it
                    if (resourcesDir != null) {
                        Util.clearCurrentContextSourceDir();
                    }
                }
            }, "script-runner");  // Use fixed thread name to avoid potential issues with special characters
            t.setDaemon(true);
            t.start();
            
        } catch (Exception ex) {
            ScriptArea output = env.getOutputArea();
            javafx.application.Platform.runLater(() -> {
                output.printlnError("ERROR loading script " + scriptName + ": " + ex.getMessage());
            });
        }
    }
    
    /**
     * Run a script file from a file path.
     * 
     * @param filePath The path to the script file
     */
    public void runScriptFile(Path filePath) {
        runScriptFile(filePath, null);
    }
    
    /**
     * Run a script file with optional project context.
     * @param filePath Path to the script file to run
     * @param projectJsonPath Optional path to project.json for loading project settings like resourceDir
     */
    public void runScriptFile(Path filePath, Path projectJsonPath) {
        try {
            if (!Files.exists(filePath)) {
                ScriptArea output = env.getOutputArea();
                javafx.application.Platform.runLater(() -> {
                    output.printlnError("ERROR: File not found: " + filePath);
                });
                return;
            }
            
            String script = Files.readString(filePath, StandardCharsets.UTF_8);
            String scriptName = filePath.getFileName().toString();
            
            // Update status bar
            if (statusBar != null) {
                javafx.application.Platform.runLater(() -> {
                    statusBar.setStatus("Running " + scriptName);
                    statusBar.clearMessage();
                });
            }
            
            // Get the directory containing the script
            Path scriptDir = filePath.getParent();
            
            // Read project settings if project.json provided
            String resourceDirPath = null;
            if (projectJsonPath != null && Files.exists(projectJsonPath)) {
                try {
                    String jsonContent = Files.readString(projectJsonPath, StandardCharsets.UTF_8);
                    Map<String, Object> projectData = (Map<String, Object>) Json.parse(jsonContent);
                    String resourceDir = (String) projectData.get("resourceDir");
                    
                    if (resourceDir != null && !resourceDir.isEmpty()) {
                        Path projectDir = projectJsonPath.getParent();
                        if (projectDir != null) {
                            Path fullResourcePath = projectDir.resolve(resourceDir).toAbsolutePath().normalize();
                            if (Files.exists(fullResourcePath) && Files.isDirectory(fullResourcePath)) {
                                resourceDirPath = fullResourcePath.toString();
                            }
                        }
                    }
                } catch (Exception ex) {
                    // Log but don't fail - resourceDir is optional
                    System.err.println("Warning: Could not read resourceDir from project.json: " + ex.getMessage());
                }
            }
            
            final String finalResourceDirPath = resourceDirPath;
            
            // Execute script in background thread
            Thread t = new Thread(() -> {
                // Set the script's directory as the context source directory
                if (scriptDir != null) {
                    Util.setCurrentContextSourceDir(scriptDir);
                }
                
                // Set resource directory if available
                if (finalResourceDirPath != null) {
                    Util.setResourceDir(Path.of(finalResourceDirPath));
                    // Also set as global variable in environment
                    env.getBaseEnvironmentValues().define("resourceDir", finalResourceDirPath);
                } else {
                    // Set empty string if no resource directory
                    env.getBaseEnvironmentValues().define("resourceDir", "");
                }
                
                try {
                    // Parse the file to get the blocks and statements with correct source path
                    RuntimeContext scriptContext = com.eb.script.parser.Parser.parse(filePath);
                    
                    // Copy debug mode state from current thread to this thread
                    boolean debugModeEnabled = com.eb.script.interpreter.screen.ScreenFactory.getDebugModeForInheritance();
                    com.eb.script.interpreter.screen.ScreenFactory.setDebugModeForThread(debugModeEnabled);
                    
                    // Create a new RuntimeContext that combines:
                    // - The correct sourcePath from scriptContext (for import resolution)
                    // - The handler's environment (to preserve screen state and runtime state)
                    // - The blocks and statements from scriptContext (the parsed script)
                    if (ctx != null) {
                        // Create a new context with sourcePath from file and environment from handler
                        RuntimeContext execContext = new RuntimeContext(
                            scriptContext.name,
                            scriptContext.sourcePath,  // Has the file path for import resolution
                            ctx.environment,            // Reuses handler's environment with screen state
                            scriptContext.blocks,       // Functions from the parsed script
                            scriptContext.statements    // Statements from the parsed script
                        );
                        
                        // Execute using the combined context
                        interpreter.interpret(execContext);
                    } else {
                        // Fallback: register output area and execute with script context
                        scriptContext.environment.registerOutputArea(env.getOutputArea());
                        interpreter.interpret(scriptContext);
                    }
                    
                    // Update status bar on completion
                    javafx.application.Platform.runLater(() -> {
                        if (statusBar != null) {
                            statusBar.clearStatus();
                            statusBar.setMessage(scriptName + " completed");
                        }
                    });
                } catch (Exception ex) {
                    // Error message
                    ScriptArea output = env.getOutputArea();
                    javafx.application.Platform.runLater(() -> {
                        output.printlnError("✗ Error running " + scriptName + ": " + Util.formatExceptionWith2Origin(ex));
                        if (statusBar != null) {
                            statusBar.clearStatus();
                            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                            String displayMsg = errorMsg.length() > MAX_ERROR_MESSAGE_LENGTH 
                                ? errorMsg.substring(0, TRUNCATED_MESSAGE_LENGTH) + "..." 
                                : errorMsg;
                            statusBar.setMessage(displayMsg, errorMsg);
                        }
                    });
                } finally {
                    // Clear the context source directory after execution
                    if (scriptDir != null) {
                        Util.clearCurrentContextSourceDir();
                    }
                    // Clear the resource directory after execution
                    if (finalResourceDirPath != null) {
                        Util.clearResourceDir();
                    }
                }
            }, "script-runner");
            t.setDaemon(true);
            t.start();
            
        } catch (Exception ex) {
            ScriptArea output = env.getOutputArea();
            javafx.application.Platform.runLater(() -> {
                output.printlnError("ERROR loading script: " + ex.getMessage());
            });
        }
    }
    
    /**
     * Create a new project with a project.json file.
     * Shows a dialog asking for project name and path.
     */
    public void createNewProject() {
        try {
            // Show new project dialog
            NewProjectDialog dialog = new NewProjectDialog(stage);
            var result = dialog.showAndWait();
            
            if (result.isEmpty()) {
                return; // User cancelled
            }
            
            NewProjectDialog.ProjectInfo projectInfo = result.get();
            String projectName = projectInfo.getName();
            String projectPath = projectInfo.getPath();
            
            // Create the project directory if it doesn't exist
            Path projectDir = Path.of(projectPath);
            if (!Files.exists(projectDir)) {
                Files.createDirectories(projectDir);
            }
            
            // Create project.json path
            Path projectJsonPath = projectDir.resolve("project.json");
            
            // Check if project.json already exists
            if (Files.exists(projectJsonPath)) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Project file already exists in this directory:\n" + projectJsonPath + "\n\nOverwrite it?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText("Project Already Exists");
                confirm.initOwner(stage);
                confirm.initModality(Modality.APPLICATION_MODAL);
                var res = confirm.showAndWait();
                if (res.isEmpty() || res.get() == ButtonType.CANCEL) {
                    return;
                }
            }
            
            // Create default project configuration with the specified name
            String projectJson = createDefaultProjectJson(projectName, projectDir.toAbsolutePath().toString());
            
            // Write project.json
            Files.writeString(projectJsonPath, projectJson, StandardCharsets.UTF_8);
            
            // Create AI instruction file
            Path instructionFilePath = projectDir.resolve(".copilot-instructions.md");
            if (!Files.exists(instructionFilePath)) {
                String instructionContent = createAiInstructionFile(projectName, projectDir.toAbsolutePath().toString());
                Files.writeString(instructionFilePath, instructionContent, StandardCharsets.UTF_8);
            }
            
            // Load the project into global environment
            loadProjectJson(projectJsonPath);
            
            // Add to project tree view
            if (projectTreeView != null) {
                projectTreeView.addProject(projectName, projectJsonPath.toString());
            }
            
            ScriptArea output = env.getOutputArea();
            output.printlnOk("New project created: " + projectJsonPath);
            output.printlnInfo("Project loaded into global variable 'project'");
            if (Files.exists(instructionFilePath)) {
                output.printlnInfo("AI instruction file created: " + instructionFilePath);
            }
            
        } catch (Exception ex) {
            submitErrors("Failed to create new project: " + ex.getMessage());
        }
    }
    
    /**
     * Open an existing project by selecting a project.json file.
     */
    public void openProject() {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Open Project");
            fc.setInitialDirectory(Util.SANDBOX_ROOT.toFile());
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Project Files", "project.json"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
            
            File projectFile = fc.showOpenDialog(stage);
            if (projectFile == null) {
                return; // User cancelled
            }
            
            Path projectJsonPath = projectFile.toPath();
            
            // Open the project
            openProjectByPath(projectJsonPath);
            
        } catch (Exception ex) {
            submitErrors("Failed to open project: " + ex.getMessage());
        }
    }
    
    /**
     * Open a project by its path.
     * Used by both the File menu and the project tree view.
     * 
     * @param projectJsonPath Path to the project.json file
     */
    public void openProjectByPath(Path projectJsonPath) {
        try {
            // Load the project into global environment
            loadProjectJson(projectJsonPath);
            
            // Extract project name from the loaded project
            Object projectObj = env.get("project");
            String projectName = "Unknown Project";
            if (projectObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> project = (Map<String, Object>) projectObj;
                Object nameObj = project.get("name");
                if (nameObj instanceof String) {
                    projectName = (String) nameObj;
                }
            }
            
            // Add to project tree view
            if (projectTreeView != null) {
                projectTreeView.addProject(projectName, projectJsonPath.toString());
            }
            
            ScriptArea output = env.getOutputArea();
            output.printlnOk("Project opened: " + projectJsonPath);
            output.printlnInfo("Project loaded into global variable 'project'");
            
        } catch (Exception ex) {
            throw new RuntimeException("Failed to open project: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Create default project.json content.
     */
    private String createDefaultProjectJson(String projectName, String projectDirectory) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"name\": \"").append(escapeJson(projectName)).append("\",\n");
        json.append("  \"directory\": \"").append(escapeJson(projectDirectory)).append("\",\n");
        json.append("  \"description\": \"EBS Script Project\",\n");
        json.append("  \"version\": \"1.0.0\",\n");
        json.append("  \"css\": [\n");
        json.append("    \"console.css\"\n");
        json.append("  ],\n");
        json.append("  \"mainScript\": \"main.ebs\",\n");
        json.append("  \"settings\": {\n");
        json.append("    \"autoLoad\": true\n");
        json.append("  }\n");
        json.append("}\n");
        return json.toString();
    }
    
    /**
     * Create AI instruction file content for a new project.
     * This file guides AI assistants on the project structure and conventions.
     */
    private String createAiInstructionFile(String projectName, String projectDirectory) {
        StringBuilder instructions = new StringBuilder();
        instructions.append("# AI Instructions for ").append(projectName).append("\n\n");
        instructions.append("## Project Overview\n\n");
        instructions.append("This is an EBS (Earl Bosch Script) project. All project configuration is stored in `project.json`.\n\n");
        instructions.append("## Project Structure\n\n");
        instructions.append("The project configuration file `project.json` contains all important project properties and directory locations.\n\n");
        instructions.append("### Key Properties in project.json\n\n");
        instructions.append("- **name**: Project name\n");
        instructions.append("- **directory**: Project root directory path\n");
        instructions.append("- **description**: Project description\n");
        instructions.append("- **version**: Project version number\n");
        instructions.append("- **mainScript**: Main entry point script file (typically `main.ebs`)\n");
        instructions.append("- **css**: Array of CSS files to load for UI styling\n");
        instructions.append("- **settings**: Custom project settings\n\n");
        instructions.append("### Directory Organization\n\n");
        instructions.append("The following directories can be configured in `project.json`:\n\n");
        instructions.append("- **resourceDir**: Directory for resource files (images, data files, etc.)\n");
        instructions.append("  - Default: `resources`\n");
        instructions.append("  - Use this for static assets and resource files\n\n");
        instructions.append("- **testDir**: Directory for test scripts\n");
        instructions.append("  - Default: `tests`\n");
        instructions.append("  - Place all test EBS scripts here\n\n");
        instructions.append("- **tempDir**: Directory for temporary files\n");
        instructions.append("  - Default: `temp`\n");
        instructions.append("  - Use this for temporary working files\n\n");
        instructions.append("- **docDir**: Directory for documentation\n");
        instructions.append("  - Default: `docs`\n");
        instructions.append("  - Place project documentation, markdown files, and guides here\n\n");
        instructions.append("## EBS Script Syntax\n\n");
        instructions.append("For complete EBS (Earl Bosch Script) language syntax and built-in functions, refer to the\n");
        instructions.append("EBS interpreter documentation:\n\n");
        instructions.append("- **EBS_LANGUAGE_REFERENCE.md** - Quick reference pointer to syntax documentation\n");
        instructions.append("- **docs/EBS_SCRIPT_SYNTAX.md** - Comprehensive syntax reference with all language features\n\n");
        instructions.append("These documentation files are located in the EBS Script Interpreter repository.\n\n");
        instructions.append("The syntax documentation includes:\n");
        instructions.append("- Data types and type casting\n");
        instructions.append("- Variables, operators, and control flow\n");
        instructions.append("- Functions, arrays, and JSON\n");
        instructions.append("- Database operations\n");
        instructions.append("- Screen/UI windows\n");
        instructions.append("- All built-in functions (string, file, HTTP, mail, CSS, array, system, etc.)\n");
        instructions.append("- Console commands and best practices\n\n");
        instructions.append("When writing EBS scripts:\n");
        instructions.append("- Refer to the syntax documentation for correct language constructs\n");
        instructions.append("- EBS is case-insensitive for all identifiers\n");
        instructions.append("- Statements typically end with semicolon `;`\n");
        instructions.append("- Use `//` for comments\n\n");
        instructions.append("## File Placement Guidelines\n\n");
        instructions.append("When working with this project:\n\n");
        instructions.append("1. **Always read `project.json` first** to understand the project structure and directory layout\n");
        instructions.append("2. **EBS Scripts**: Place in the project root or organize in subdirectories\n");
        instructions.append("3. **Resources**: Use the directory specified in `resourceDir` property\n");
        instructions.append("4. **Tests**: Use the directory specified in `testDir` property\n");
        instructions.append("5. **Documentation**: Use the directory specified in `docDir` property\n");
        instructions.append("6. **Temporary Files**: Use the directory specified in `tempDir` property\n");
        instructions.append("7. **CSS Files**: Reference in the `css` array in project.json\n\n");
        instructions.append("## Example project.json Structure\n\n");
        instructions.append("```json\n");
        instructions.append("{\n");
        instructions.append("  \"name\": \"").append(escapeJson(projectName)).append("\",\n");
        instructions.append("  \"directory\": \"").append(escapeJson(projectDirectory)).append("\",\n");
        instructions.append("  \"description\": \"EBS Script Project\",\n");
        instructions.append("  \"version\": \"1.0.0\",\n");
        instructions.append("  \"mainScript\": \"main.ebs\",\n");
        instructions.append("  \"css\": [\"console.css\"],\n");
        instructions.append("  \"resourceDir\": \"resources\",\n");
        instructions.append("  \"testDir\": \"tests\",\n");
        instructions.append("  \"tempDir\": \"temp\",\n");
        instructions.append("  \"docDir\": \"docs\",\n");
        instructions.append("  \"settings\": {\n");
        instructions.append("    \"autoLoad\": true\n");
        instructions.append("  }\n");
        instructions.append("}\n");
        instructions.append("```\n\n");
        instructions.append("## Working with AI Assistants\n\n");
        instructions.append("When asking AI to create or modify files:\n");
        instructions.append("- Specify which directory the file belongs in based on project.json\n");
        instructions.append("- AI should check project.json to determine correct file locations\n");
        instructions.append("- All paths in project.json are relative to the project root directory\n");
        return instructions.toString();
    }
    
    /**
     * Load project.json into global environment and apply CSS files.
     */
    private void loadProjectJson(Path projectJsonPath) throws Exception {
        // Read and parse project.json
        String jsonContent = Files.readString(projectJsonPath, StandardCharsets.UTF_8);
        Object projectObj = Json.parse(jsonContent);
        
        if (!(projectObj instanceof Map)) {
            throw new Exception("Invalid project.json: root must be a JSON object");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> project = (Map<String, Object>) projectObj;
        
        // Store project in global environment
        env.getBaseEnvironmentValues().define("project", project);
        
        // Apply CSS files if specified
        if (project.containsKey("css")) {
            Object cssObj = project.get("css");
            if (cssObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> cssList = (java.util.List<Object>) cssObj;
                applyCssFiles(cssList, projectJsonPath.getParent());
            }
        }
    }
    
    /**
     * Apply CSS files to the main scene.
     */
    private void applyCssFiles(java.util.List<Object> cssList, Path projectDir) {
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.Scene scene = stage.getScene();
                if (scene == null) {
                    return;
                }
                
                for (Object cssObj : cssList) {
                    if (cssObj instanceof String) {
                        String cssPath = (String) cssObj;
                        
                        // Try to resolve CSS path relative to project directory
                        Path cssFile = projectDir.resolve(cssPath);
                        String cssUrl = null;
                        
                        if (Files.exists(cssFile)) {
                            // File exists in project directory
                            cssUrl = cssFile.toUri().toString();
                        } else {
                            // Try as classpath resource
                            // Only prepend /css/ if the path doesn't already contain a directory structure
                            String resourcePath = cssPath;
                            if (!resourcePath.startsWith("/")) {
                                resourcePath = "/" + resourcePath;
                            }
                            // Only add /css/ prefix if path is a simple filename without directory separators
                            // (i.e., no '/' after the first character)
                            if (resourcePath.indexOf('/', 1) < 0 && !resourcePath.startsWith("/css/")) {
                                resourcePath = "/css" + resourcePath;
                            }
                            
                            java.net.URL resource = getClass().getResource(resourcePath);
                            if (resource != null) {
                                cssUrl = resource.toExternalForm();
                            }
                        }
                        
                        if (cssUrl != null) {
                            // Normalize URL for comparison by converting to URI and back
                            try {
                                java.net.URI normalizedUri = new java.net.URI(cssUrl).normalize();
                                String normalizedUrl = normalizedUri.toString();
                                
                                // Check if already loaded using normalized URL
                                boolean alreadyLoaded = false;
                                for (String existingUrl : scene.getStylesheets()) {
                                    try {
                                        String existingNormalized = new java.net.URI(existingUrl).normalize().toString();
                                        if (existingNormalized.equals(normalizedUrl)) {
                                            alreadyLoaded = true;
                                            break;
                                        }
                                    } catch (Exception e) {
                                        // If normalization fails, compare directly
                                        System.err.println("Warning: Could not normalize URL for comparison: " + e.getMessage());
                                        if (existingUrl.equals(cssUrl)) {
                                            alreadyLoaded = true;
                                            break;
                                        }
                                    }
                                }
                                
                                if (!alreadyLoaded) {
                                    scene.getStylesheets().add(cssUrl);
                                    ScriptArea output = env.getOutputArea();
                                    if (output != null) {
                                        output.printlnInfo("CSS loaded: " + cssPath);
                                    }
                                }
                            } catch (java.net.URISyntaxException e) {
                                // Fallback to simple contains check if URI parsing fails
                                if (!scene.getStylesheets().contains(cssUrl)) {
                                    scene.getStylesheets().add(cssUrl);
                                    ScriptArea output = env.getOutputArea();
                                    if (output != null) {
                                        output.printlnInfo("CSS loaded: " + cssPath);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ScriptArea output = env.getOutputArea();
                if (output != null) {
                    output.printlnWarn("Failed to load some CSS files: " + ex.getMessage());
                }
            }
        });
    }
    
    /**
     * Escape special characters for JSON strings.
     * Handles standard JSON escape sequences including control characters.
     */
    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    result.append("\\\\");
                    break;
                case '"':
                    result.append("\\\"");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    // Escape control characters as unicode escapes
                    if (c < 0x20 || c == 0x7F) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        return result.toString();
    }
    
    /**
     * Create a new file in a project.
     * Shows a dialog asking for file type, name, and path.
     * 
     * @param projectJsonPath The path to the project.json file
     */
    public void createNewFile(String projectJsonPath) {
        try {
            // Extract project directory from project.json path
            Path jsonPath = Path.of(projectJsonPath);
            Path projectDir = jsonPath.getParent();
            String projectPath = projectDir != null ? projectDir.toString() : projectJsonPath;
            
            // Show new file dialog
            NewFileDialog dialog = new NewFileDialog(stage, projectPath);
            var result = dialog.showAndWait();
            
            if (result.isEmpty()) {
                return; // User cancelled
            }
            
            NewFileDialog.FileInfo fileInfo = result.get();
            String fullPath = fileInfo.getFullPath();
            Path filePath = Path.of(fullPath);
            
            // Create directory if it doesn't exist
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Check if file already exists
            if (Files.exists(filePath)) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "File already exists:\n" + fullPath + "\n\nOverwrite it?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText("File Already Exists");
                confirm.initOwner(stage);
                confirm.initModality(Modality.APPLICATION_MODAL);
                var res = confirm.showAndWait();
                if (res.isEmpty() || res.get() == ButtonType.CANCEL) {
                    return;
                }
            }
            
            // Create file with appropriate default content based on type
            String defaultContent = getDefaultContentForFileType(fileInfo.getType());
            Files.writeString(filePath, defaultContent, StandardCharsets.UTF_8);
            
            // DO NOT add file to project.json - just create it on filesystem
            // Tree view will automatically show it when refreshed
            
            // Open the file in a tab using the same approach as /open command
            Path p = Util.resolveSandboxedPath(fullPath);
            String handle = (String) Builtins.callBuiltin(env, "file.open", fullPath, "rw");
            FileContext ofile = new FileContext(handle, p, "rw");
            tabHandler.showTab(new TabContext(p.getFileName().toString(), p, ofile), true);
            
            ScriptArea output = env.getOutputArea();
            output.printlnOk("File created: " + fullPath);
            
            // Refresh the tree view to show the new file
            if (projectTreeView != null) {
                projectTreeView.refreshProjectFiles(projectJsonPath);
            }
            
        } catch (Exception ex) {
            submitErrors("Failed to create new file: " + ex.getMessage());
        }
    }
    
    /**
     * Add an existing file to the project by opening it in a tab.
     * Shows a file chooser dialog.
     * 
     * @param projectJsonPath The path to the project.json file
     */
    public void addExistingFile(String projectJsonPath) {
        try {
            // Extract project directory from project.json path
            Path jsonPath = Path.of(projectJsonPath);
            Path projectDir = jsonPath.getParent();
            String projectPath = projectDir != null ? projectDir.toString() : projectJsonPath;
            
            FileChooser fc = new FileChooser();
            fc.setTitle("Add Existing File");
            
            // Set initial directory to project path
            File initialDir = new File(projectPath);
            if (initialDir.exists() && initialDir.isDirectory()) {
                fc.setInitialDirectory(initialDir);
            } else {
                fc.setInitialDirectory(Util.SANDBOX_ROOT.toFile());
            }
            
            // Add file type filters
            fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("EBS Scripts", "*.ebs"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("CSS Files", "*.css"),
                new FileChooser.ExtensionFilter("Markdown Files", "*.md"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            File selectedFile = fc.showOpenDialog(stage);
            if (selectedFile == null) {
                return; // User cancelled
            }
            
            Path filePath = selectedFile.toPath();
            String fullPath = filePath.toString();
            
            // Add to MRU
            addRecentFile(filePath);
            
            // Add file to project.json
            if (Files.exists(jsonPath)) {
                addFileToProjectJson(jsonPath, filePath);
            }
            
            // Open the file in a tab using the same approach as /open command
            Path p = Util.resolveSandboxedPath(fullPath);
            String handle = (String) Builtins.callBuiltin(env, "file.open", fullPath, "rw");
            FileContext ofile = new FileContext(handle, p, "rw");
            tabHandler.showTab(new TabContext(p.getFileName().toString(), p, ofile), true);
            
            ScriptArea output = env.getOutputArea();
            output.printlnOk("File opened: " + fullPath);
            
        } catch (Exception ex) {
            submitErrors("Failed to add file: " + ex.getMessage());
        }
    }
    
    /**
     * Get default content for a file based on its type.
     */
    private String getDefaultContentForFileType(NewFileDialog.FileType fileType) {
        return switch (fileType) {
            case EBS_SCRIPT -> "// EBS Script\n// Type your code here\n\n";
            case JSON -> "{\n  \n}\n";
            case CSS -> "/* CSS Styles */\n\n";
            case MARKDOWN -> "# Markdown Document\n\n";
        };
    }
    
    /**
     * Open a file from the tree view.
     * 
     * @param filePath Path to the file to open
     */
    public void openFileFromTreeView(Path filePath) {
        try {
            String fullPath = filePath.toString();
            
            // Add to MRU
            addRecentFile(filePath);
            
            // Check if file is already open in a tab
            Path p = Util.resolveSandboxedPath(fullPath);
            Tab existingTab = tabHandler.findTabByPath(p);
            
            if (existingTab != null) {
                // File is already open, just select the tab
                tabHandler.selectTab(existingTab);
                ScriptArea output = env.getOutputArea();
                output.printlnOk("Switched to file: " + fullPath);
            } else {
                // Open the file in a new tab
                String handle = (String) Builtins.callBuiltin(env, "file.open", fullPath, "rw");
                FileContext ofile = new FileContext(handle, p, "rw");
                tabHandler.showTab(new TabContext(p.getFileName().toString(), p, ofile), true);
                
                ScriptArea output = env.getOutputArea();
                output.printlnOk("File opened: " + fullPath);
            }
            
        } catch (Exception ex) {
            submitErrors("Failed to open file: " + ex.getMessage());
        }
    }
    
    /**
     * Add a file to the project.json file and update the tree view.
     * 
     * @param projectJsonPath Path to the project.json file
     * @param filePath Path to the file to add
     */
    private void addFileToProjectJson(Path projectJsonPath, Path filePath) {
        try {
            // Read and parse project.json
            String jsonContent = Files.readString(projectJsonPath, StandardCharsets.UTF_8);
            Object projectObj = Json.parse(jsonContent);
            
            if (!(projectObj instanceof Map)) {
                System.err.println("Invalid project.json: root must be a JSON object");
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) projectObj;
            
            // Get or create files array
            Object filesObj = project.get("files");
            java.util.List<String> filesList = null;
            
            // Handle ArrayDynamic (used by EBS JSON parser) or standard List
            if (filesObj instanceof com.eb.script.arrays.ArrayDynamic) {
                com.eb.script.arrays.ArrayDynamic arrayDynamic = (com.eb.script.arrays.ArrayDynamic) filesObj;
                filesList = new java.util.ArrayList<>();
                for (int i = 0; i < arrayDynamic.size(); i++) {
                    Object item = arrayDynamic.get(i);
                    if (item instanceof String) {
                        filesList.add((String) item);
                    }
                }
            } else if (filesObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> list = (java.util.List<String>) filesObj;
                filesList = new java.util.ArrayList<>(list); // Create mutable copy
            }
            
            if (filesList == null) {
                filesList = new java.util.ArrayList<>();
            }
            
            // Make file path relative to project directory if possible
            Path projectDir = projectJsonPath.getParent();
            String relativePath;
            try {
                relativePath = projectDir.relativize(filePath).toString();
            } catch (IllegalArgumentException e) {
                // If files are on different drives or can't be relativized, use absolute path
                relativePath = filePath.toString();
            }
            
            // Add file if not already in list
            if (!filesList.contains(relativePath)) {
                filesList.add(relativePath);
                
                // Update the project map with the modified files list
                project.put("files", filesList);
                
                // Write updated project.json
                String updatedJson = Json.prettyJson(project);
                Files.writeString(projectJsonPath, updatedJson, StandardCharsets.UTF_8);
                
                // Refresh the tree view
                if (projectTreeView != null) {
                    projectTreeView.refreshProjectFiles(projectJsonPath.toString());
                }
                
                System.out.println("Added file to project.json: " + relativePath);
            }
            
        } catch (Exception ex) {
            System.err.println("Failed to add file to project.json: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Create a new file in a specific directory within a project.
     * Shows a dialog asking for file type, name, and path.
     * 
     * @param projectJsonPath The path to the project.json file
     * @param directoryPath The path to the directory where the file should be created
     */
    public void createNewFileInDirectory(String projectJsonPath, String directoryPath) {
        try {
            // Show new file dialog with the directory as default path
            NewFileDialog dialog = new NewFileDialog(stage, directoryPath);
            var result = dialog.showAndWait();
            
            if (result.isEmpty()) {
                return; // User cancelled
            }
            
            NewFileDialog.FileInfo fileInfo = result.get();
            String fullPath = fileInfo.getFullPath();
            Path filePath = Path.of(fullPath);
            
            // Create directory if it doesn't exist
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Check if file already exists
            if (Files.exists(filePath)) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "File already exists:\n" + fullPath + "\n\nOverwrite it?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText("File Already Exists");
                confirm.initOwner(stage);
                confirm.initModality(Modality.APPLICATION_MODAL);
                var res = confirm.showAndWait();
                if (res.isEmpty() || res.get() == ButtonType.CANCEL) {
                    return;
                }
            }
            
            // Create file with appropriate default content based on type
            String defaultContent = getDefaultContentForFileType(fileInfo.getType());
            Files.writeString(filePath, defaultContent, StandardCharsets.UTF_8);
            
            // DO NOT add file to project.json - just create it on filesystem
            // Tree view will automatically show it when refreshed
            
            // Open the file in a tab using the same approach as /open command
            Path p = Util.resolveSandboxedPath(fullPath);
            String handle = (String) Builtins.callBuiltin(env, "file.open", fullPath, "rw");
            FileContext ofile = new FileContext(handle, p, "rw");
            tabHandler.showTab(new TabContext(p.getFileName().toString(), p, ofile), true);
            
            ScriptArea output = env.getOutputArea();
            output.printlnOk("File created: " + fullPath);
            
            // Refresh the tree view
            if (projectTreeView != null) {
                projectTreeView.refreshProjectFiles(projectJsonPath);
            }
            
        } catch (Exception ex) {
            submitErrors("Failed to create new file: " + ex.getMessage());
        }
    }
}
