package com.eb.ui.ebs;

import com.eb.script.RuntimeContext;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.file.FileContext;
import com.eb.script.interpreter.Builtins;
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

    protected final Stage stage;
    protected final Deque<Path> recentFiles = new ArrayDeque<>(); // Most recent at the head
    private int newScriptSequence = 1; // Sequence number for new script files

    public EbsConsoleHandler(Stage stage, RuntimeContext ctx) {
        super(ctx);
        this.stage = stage;
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
                if (line.charAt(0) == '/') {
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

    private void displayDetailedHelp(ScriptArea output, String itemName) {
        try {
            // Load system-lookup.json from resources
            InputStream is = getClass().getClassLoader().getResourceAsStream("system-lookup.json");
            if (is == null) {
                output.printlnWarn("Help system not available (system-lookup.json not found)");
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
                String displayMsg = lastError.length() > 60 
                    ? lastError.substring(0, 57) + "..." 
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
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
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
                    if (contex.path.toFile().isFile()) {
                        fc.setInitialDirectory(contex.path.getParent().toFile());
                    } else {
                        fc.setInitialDirectory(contex.path.toFile());
                    }
                    if (contex.path.getFileName() != null) {
                        fc.setInitialFileName(contex.path.getFileName().toString());
                    }
                } else {
                    fc.setInitialDirectory(Util.SANDBOX_ROOT.toFile());
                }

                File out = fc.showSaveDialog(stage);
                if (out != null) {
                    addRecentFile(out.toPath());
                    FileContext fileContext = new FileContext(null, out.toPath(), "r");
                    contex.fileContext = fileContext;
                    if (!fileContext.closed) {
                        callBuiltin("file.close", fileContext.handle);
                    }
                    callBuiltin("file.writeTextFile", contex.path.toString(), tab.getEditorText());
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
     * Create a new empty script file with a default name "newScript_x.ebs"
     * where x is an incrementing sequence number.
     * The file is NOT created on disk - it exists only in the tab until saved.
     */
    public void createNewScriptFile() {
        try {
            String filename = getNextNewScriptFilename();
            Path path = Util.SANDBOX_ROOT.resolve(filename);
            
            // DO NOT create the physical file - just create the tab
            // Create a TabContext with a null fileContext since the file doesn't exist yet
            TabContext tabContext = new TabContext(filename, path, null);
            
            // Create the tab using the tab handler
            EbsTab newTab = tabHandler.createNewTab(tabContext, true);
            
            if (newTab != null) {
                // Initialize with default content and mark as dirty (unsaved)
                String defaultContent = "// New EBS Script\n\n";
                newTab.initializeAsNewFile(defaultContent);
            }
        } catch (Exception ex) {
            submitErrors("Failed to create new script file: " + ex.getMessage());
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
                // Save the file
                TabContext context = (TabContext) tab.getUserData();
                if (context != null && context.path != null && Files.exists(context.path)) {
                    // File already exists, just save
                    saveHandle(tab);
                } else {
                    // File doesn't exist yet, use Save As
                    chooseSaveAs(tab);
                }
                return true; // Close after saving
            } else if (result.get() == saveAsButton) {
                // Save As
                chooseSaveAs(tab);
                return true; // Close after saving
            } else if (result.get() == dontSaveButton) {
                // Don't save, just close
                return true;
            } else {
                // Cancel was pressed
                return false;
            }
        }

        // Dialog was closed without a selection, treat as cancel
        return false;
    }
}
