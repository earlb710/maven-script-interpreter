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
            if (args != null && args.length > 0) {
                String script = args[0];
                RuntimeContext runtime = Parser.parse(Path.of(script));

                Interpreter interpreter = new Interpreter();
                interpreter.interpret(runtime);

//                System.out.print("> ");
//                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//                String name = reader.readLine();
//                System.out.println("Hello, " + name + "!");
            }
        } catch (InterpreterError e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (Interpreter.ReturnSignal e) {
            System.out.println("Return:" + e.value);
        } catch (Interpreter.BreakSignal e) {
            System.out.println("Execution interupted : Break");
        } catch (IOException ex) {
            System.getLogger(Run.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
}
