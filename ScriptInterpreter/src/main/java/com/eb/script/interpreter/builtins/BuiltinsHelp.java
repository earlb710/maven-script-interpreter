package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;

import com.eb.script.arrays.ArrayDef;
import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.json.Json;
import com.eb.script.token.Category;
import com.eb.script.token.DataType;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.ui.cli.ScriptArea;
import com.eb.util.Debugger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Built-in functions for Help operations.
 * Handles system.help, system.inputDialog, system.confirmDialog, system.alertDialog builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsHelp {

    /**
     * Dispatch a Help/Dialog builtin by name.
     *
     * @param env The execution environment
     * @param name Lowercase builtin name (e.g., "system.help")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(Environment env, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "system.help" -> help(env, args);
            case "system.inputdialog" -> inputDialog(args);
            case "system.confirmdialog" -> confirmDialog(args);
            case "system.alertdialog" -> alertDialog(args);
            default -> throw new InterpreterError("Unknown Help builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Help/Dialog builtin.
     */
    public static boolean handles(String name) {
        return name.equals("system.help") || 
               name.equals("system.inputdialog") || 
               name.equals("system.confirmdialog") || 
               name.equals("system.alertdialog");
    }

    // --- Individual builtin implementations ---

    private static Object help(Environment env, Object[] args) throws InterpreterError {
        Debugger debug = env.getDebugger();
        ScriptArea output = env.getOutputArea();

        String keyword = args.length > 0 && args[0] != null ? args[0].toString() : null;

        if (keyword != null && !keyword.isEmpty()) {
            return getDetailedHelp(keyword);
        }

        final Set<String> keywords = new TreeSet<>();
        final Set<String> operators = new TreeSet<>();
        final Set<String> datatypes = new TreeSet<>();
        for (EbsTokenType t : EbsTokenType.values()) {
            if (t.getCategory() == Category.KEYWORD) {
                for (String s : t.getStrings()) {
                    if (s != null && !s.isEmpty()) {
                        keywords.add(s);
                    }
                }
            }
            if (t.getCategory() == Category.OPERATOR) {
                for (String s : t.getStrings()) {
                    if (s != null && !s.isEmpty()) {
                        operators.add(s);
                    }
                }
            }
            if (t.getDataType() != null) {
                for (String s : t.getStrings()) {
                    if (s != null && !s.isEmpty()) {
                        datatypes.add(s);
                    }
                }
            }
        }

        final List<Map<String, Object>> builtins = new ArrayList<>();
        for (String bname : Builtins.NAMES) {
            Builtins.BuiltinInfo info = Builtins.getBuiltinInfo(bname);
            Map<String, Object> bi = new java.util.LinkedHashMap<>();
            bi.put("name", info.name);
            bi.put("returnType", (info != null && info.returnType != null) ? info.returnType.name() : null);

            List<Map<String, Object>> params = new ArrayList<>();
            if (info != null && info.params != null) {
                for (com.eb.script.interpreter.statement.Parameter p : info.params) {
                    Map<String, Object> pm = new java.util.LinkedHashMap<>();
                    pm.put("name", p.name);
                    pm.put("type", p.paramType == null ? null : p.paramType.name());
                    pm.put("mandatory", p.mandatory);
                    params.add(pm);
                }
            }
            bi.put("params", params);
            builtins.add(bi);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Keywords:\n");
        for (String k : keywords) {
            sb.append("  ").append(k).append('\n');
        }
        sb.append('\n').append("Datatypes:\n");
        for (String d : datatypes) {
            sb.append("  ").append(d).append('\n');
        }
        sb.append('\n').append("Operators:\n");
        for (String o : operators) {
            sb.append("  ").append(o).append('\n');
        }
        sb.append('\n').append("Builtins:\n");
        for (var bi : builtins) {
            sb.append("  ").append(bi.get("name")).append("(");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ps = (List<Map<String, Object>>) bi.get("params");
            if (ps != null) {
                for (int i = 0; i < ps.size(); i++) {
                    var pm = ps.get(i);
                    String pname = (String) pm.get("name");
                    String ptype = (String) pm.get("type");
                    boolean mand = Boolean.TRUE.equals(pm.get("mandatory"));
                    boolean arr = Boolean.TRUE.equals(pm.get("array"));
                    sb.append(pname).append(":").append(ptype == null ? "any" : ptype.toLowerCase());
                    if (arr) {
                        sb.append("[]");
                    }
                    if (!mand) {
                        sb.append("?");
                    }
                    if (i < ps.size() - 1) {
                        sb.append(", ");
                    }
                }
            }
            String returnType = (String) bi.get("returnType");
            sb.append(") : ").append(returnType == null ? "null" : returnType.toLowerCase()).append('\n');
        }
        final String text = sb.toString();

        if (debug != null && debug.isDebugOn()) {
            for (String line : text.split("\\r?\\n")) {
                if (!line.isEmpty()) {
                    debug.debugWriteLine("INFO", line);
                }
            }
        } else {
            if (env.isEchoOn()) {
                System.out.print(text);
            }
        }

        return text;
    }

    private static Object inputDialog(Object[] args) throws InterpreterError {
        String title = args.length > 0 && args[0] != null ? args[0].toString() : "Input";
        String headerText = args.length > 1 && args[1] != null ? args[1].toString() : null;
        String defaultValue = args.length > 2 && args[2] != null ? args[2].toString() : "";

        final java.util.concurrent.atomic.AtomicReference<String> resultRef = new java.util.concurrent.atomic.AtomicReference<>("");
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(defaultValue);
            dialog.setTitle(title);
            if (headerText != null && !headerText.isEmpty()) {
                dialog.setHeaderText(headerText);
            }
            dialog.setContentText("Enter value:");

            java.util.Optional<String> result = dialog.showAndWait();
            resultRef.set(result.orElse(""));
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("system.inputDialog interrupted: " + e.getMessage());
        }
        return resultRef.get();
    }

    private static Object confirmDialog(Object[] args) throws InterpreterError {
        String message = args.length > 0 && args[0] != null ? args[0].toString() : "Are you sure?";
        String title = args.length > 1 && args[1] != null ? args[1].toString() : "Confirm";
        String headerText = args.length > 2 && args[2] != null ? args[2].toString() : null;

        final java.util.concurrent.atomic.AtomicBoolean resultRef = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                message,
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.NO
            );
            confirm.setTitle(title);
            if (headerText != null && !headerText.isEmpty()) {
                confirm.setHeaderText(headerText);
            }

            java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
            resultRef.set(result.isPresent() && result.get() == javafx.scene.control.ButtonType.YES);
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("system.confirmDialog interrupted: " + e.getMessage());
        }
        return resultRef.get();
    }

    private static Object alertDialog(Object[] args) throws InterpreterError {
        String message = args.length > 0 && args[0] != null ? args[0].toString() : "";
        String title = args.length > 1 && args[1] != null ? args[1].toString() : "Alert";
        String alertTypeStr = args.length > 2 && args[2] != null ? args[2].toString().toLowerCase() : "info";

        javafx.scene.control.Alert.AlertType alertType;
        switch (alertTypeStr) {
            case "warning" -> alertType = javafx.scene.control.Alert.AlertType.WARNING;
            case "error" -> alertType = javafx.scene.control.Alert.AlertType.ERROR;
            default -> alertType = javafx.scene.control.Alert.AlertType.INFORMATION;
        }

        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(alertType, message);
            alert.setTitle(title);
            alert.showAndWait();
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterpreterError("system.alertDialog interrupted: " + e.getMessage());
        }
        return null; // Message dialog returns nothing
    }

    // --- Helper methods ---

    @SuppressWarnings("unchecked")
    private static String getDetailedHelp(String itemName) {
        try {
            java.io.InputStream is = Builtins.class.getClassLoader().getResourceAsStream("help-lookup.json");
            if (is == null) {
                return "Help system not available (help-lookup.json not found)";
            }

            String jsonContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            is.close();

            Map<String, Object> lookup = (Map<String, Object>) Json.parse(jsonContent);

            ArrayDynamic keywords = (ArrayDynamic) lookup.get("keywords");
            if (keywords != null) {
                for (Object keywordObj : keywords) {
                    Map<String, Object> keyword = (Map<String, Object>) keywordObj;
                    String kwName = (String) keyword.get("keyword");
                    if (kwName != null && kwName.equalsIgnoreCase(itemName)) {
                        return formatHelpEntry(kwName, keyword, "Keyword");
                    }
                }
            }

            ArrayDynamic builtins = (ArrayDynamic) lookup.get("builtins");
            if (builtins != null) {
                for (Object builtinObj : builtins) {
                    Map<String, Object> builtin = (Map<String, Object>) builtinObj;
                    String funcName = (String) builtin.get("function");
                    if (funcName != null && funcName.equalsIgnoreCase(itemName)) {
                        return formatHelpEntry(funcName, builtin, "Builtin Function");
                    }
                }
            }

            return "No help found for: " + itemName + "\nUse system.help() or /help keywords to see all available keywords and builtins.";

        } catch (Exception ex) {
            return "Error loading help: " + ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private static String formatHelpEntry(String name, Map<String, Object> entry, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append(type).append(": ").append(name).append("\n");
        sb.append("═══════════════════════════════════════════════════════════\n");

        String shortDesc = (String) entry.get("short_description");
        if (shortDesc != null && !shortDesc.isEmpty()) {
            sb.append("Description: ").append(shortDesc).append("\n\n");
        }

        if (entry.containsKey("parameters")) {
            ArrayDynamic params = (ArrayDynamic) entry.get("parameters");
            if (params != null && !params.isEmpty()) {
                StringBuilder paramStr = new StringBuilder();
                boolean first = true;
                for (Object param : params) {
                    if (!first) {
                        paramStr.append(", ");
                    }
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
                        paramStr.append(param.toString());
                    }
                    first = false;
                }
                sb.append("Parameters: ").append(paramStr.toString()).append("\n");
            }
        }

        if (entry.containsKey("return_type")) {
            String returnType = (String) entry.get("return_type");
            if (returnType != null && !returnType.isEmpty()) {
                sb.append("Returns: ").append(returnType).append("\n");
            }
        }

        if (entry.containsKey("parameters") || entry.containsKey("return_type")) {
            sb.append("\n");
        }

        String longHelp = (String) entry.get("long_help");
        if (longHelp != null && !longHelp.isEmpty()) {
            sb.append("Details:\n");
            sb.append(longHelp).append("\n\n");
        }

        String example = (String) entry.get("example");
        if (example != null && !example.isEmpty()) {
            sb.append("Example:\n");
            sb.append(example).append("\n");
        }

        sb.append("═══════════════════════════════════════════════════════════\n");
        return sb.toString();
    }
}
