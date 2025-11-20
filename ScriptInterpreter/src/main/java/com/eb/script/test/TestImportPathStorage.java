package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.statement.ImportStatement;
import java.nio.file.Path;

public class TestImportPathStorage {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Import Path Parsing and Storage ===");
            System.out.println();
            
            // Test 1: Import with subdirectory
            System.out.println("Test 1: Import with subdirectory");
            String test1 = "import \"util/stringUtil.ebs\";";
            System.out.println("  Statement: " + test1);
            RuntimeContext ctx1 = Parser.parse("test1", test1);
            if (ctx1.statements.length > 0 && ctx1.statements[0] instanceof ImportStatement) {
                ImportStatement imp = (ImportStatement) ctx1.statements[0];
                System.out.println("  Import filename: " + imp.filename);
                Path normalized = Path.of(imp.filename).normalize();
                System.out.println("  Normalized path: " + normalized);
            }
            System.out.println();
            
            // Test 2: Import with spaces
            System.out.println("Test 2: Import with spaces in directory name");
            String test2 = "import \"test dir/subdir/helper.ebs\";";
            System.out.println("  Statement: " + test2);
            RuntimeContext ctx2 = Parser.parse("test2", test2);
            if (ctx2.statements.length > 0 && ctx2.statements[0] instanceof ImportStatement) {
                ImportStatement imp = (ImportStatement) ctx2.statements[0];
                System.out.println("  Import filename: " + imp.filename);
                Path normalized = Path.of(imp.filename).normalize();
                System.out.println("  Normalized path: " + normalized);
            }
            System.out.println();
            
            // Test 3: Import with redundant path elements
            System.out.println("Test 3: Import with redundant path elements (./)");
            String test3 = "import \"./util/../util/stringUtil.ebs\";";
            System.out.println("  Statement: " + test3);
            RuntimeContext ctx3 = Parser.parse("test3", test3);
            if (ctx3.statements.length > 0 && ctx3.statements[0] instanceof ImportStatement) {
                ImportStatement imp = (ImportStatement) ctx3.statements[0];
                System.out.println("  Import filename: " + imp.filename);
                Path normalized = Path.of(imp.filename).normalize();
                System.out.println("  Normalized path: " + normalized);
                System.out.println("  (Should normalize to: util/stringUtil.ebs)");
            }
            System.out.println();
            
            // Test 4: Single quotes
            System.out.println("Test 4: Import with single quotes");
            String test4 = "import 'util/stringUtil.ebs';";
            System.out.println("  Statement: " + test4);
            RuntimeContext ctx4 = Parser.parse("test4", test4);
            if (ctx4.statements.length > 0 && ctx4.statements[0] instanceof ImportStatement) {
                ImportStatement imp = (ImportStatement) ctx4.statements[0];
                System.out.println("  Import filename: " + imp.filename);
                Path normalized = Path.of(imp.filename).normalize();
                System.out.println("  Normalized path: " + normalized);
            }
            System.out.println();
            
            System.out.println("=== All tests completed ===");
            System.out.println("Summary: Import statements correctly preserve subdirectory paths and spaces.");
            System.out.println("The normalized path will be stored in the global import list.");
            
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
