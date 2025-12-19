package com.eb.script;

import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.parser.ParseError;
import com.eb.script.parser.Parser;
import java.io.IOException;
import java.nio.file.Path;

public class Run {
 
    public static void main(String[] args) throws ParseError, InterpreterError {
        try {
            if (args == null || args.length == 0) {
                printUsage();
                return;
            }
            
            // Check for parse-only flag
            boolean parseOnly = false;
            String scriptFile = null;
            
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--parse") || arg.equals("-p")) {
                    parseOnly = true;
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    printUsage();
                    return;
                } else if (!arg.startsWith("-")) {
                    scriptFile = arg;
                }
            }
            
            if (scriptFile == null) {
                System.err.println("Error: No script file specified");
                printUsage();
                System.exit(1);
            }
            
            // Parse the script (this also validates imports)
            RuntimeContext runtime = Parser.parse(Path.of(scriptFile));
            
            if (parseOnly) {
                // Parse-only mode: just validate syntax and imports
                System.out.println("Parse successful: " + scriptFile);
                System.out.println("No syntax errors found.");
                return;
            }
            
            // Normal execution mode
            Interpreter interpreter = new Interpreter();
            interpreter.interpret(runtime);

//                System.out.print("> ");
//                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//                String name = reader.readLine();
//                System.out.println("Hello, " + name + "!");
        } catch (ParseError e) {
            System.err.println("Parse error: " + e.getMessage());
            System.exit(1);
        } catch (InterpreterError e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (Interpreter.ReturnSignal e) {
            System.out.println("Return:" + e.value);
        } catch (Interpreter.BreakSignal e) {
            System.out.println("Execution interupted : Break");
        } catch (IOException ex) {
            System.getLogger(Run.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java com.eb.script.Run [options] <script-file>");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -p, --parse    Parse and validate syntax only (do not execute)");
        System.out.println("  -h, --help     Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java com.eb.script.Run script.ebs           # Execute script");
        System.out.println("  java com.eb.script.Run --parse script.ebs   # Validate syntax only");
    }
}
