package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.parser.ParseError;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.statement.Statement;
import com.eb.script.interpreter.statement.ImportStatement;

public class TestImportParserInterpreter {
    public static void main(String[] args) {
        try {
            // Test 1: Import with subdirectory
            String test1 = "import \"util/stringUtil.ebs\";";
            System.out.println("Test 1: Parsing: " + test1);
            RuntimeContext ctx1 = Parser.parse("test1", test1);
            Statement[] stmts1 = ctx1.statements;
            if (stmts1.length > 0 && stmts1[0] instanceof ImportStatement) {
                ImportStatement imp1 = (ImportStatement) stmts1[0];
                System.out.println("  SUCCESS: Import filename = " + imp1.filename);
            } else {
                System.out.println("  FAILED: Not an import statement");
            }
            System.out.println();
            
            // Test 2: Import with spaces in name (double quotes)
            String test2 = "import \"my utils/string util.ebs\";";
            System.out.println("Test 2: Parsing: " + test2);
            RuntimeContext ctx2 = Parser.parse("test2", test2);
            Statement[] stmts2 = ctx2.statements;
            if (stmts2.length > 0 && stmts2[0] instanceof ImportStatement) {
                ImportStatement imp2 = (ImportStatement) stmts2[0];
                System.out.println("  SUCCESS: Import filename = " + imp2.filename);
            } else {
                System.out.println("  FAILED: Not an import statement");
            }
            System.out.println();
            
            // Test 3: Import with single quotes
            String test3 = "import 'util/stringUtil.ebs';";
            System.out.println("Test 3: Parsing: " + test3);
            RuntimeContext ctx3 = Parser.parse("test3", test3);
            Statement[] stmts3 = ctx3.statements;
            if (stmts3.length > 0 && stmts3[0] instanceof ImportStatement) {
                ImportStatement imp3 = (ImportStatement) stmts3[0];
                System.out.println("  SUCCESS: Import filename = " + imp3.filename);
            } else {
                System.out.println("  FAILED: Not an import statement");
            }
            
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
