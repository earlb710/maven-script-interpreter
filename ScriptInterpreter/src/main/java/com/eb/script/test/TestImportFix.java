package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import java.nio.file.Path;

public class TestImportFix {
    public static void main(String[] args) {
        try {
            System.out.println("Testing import_examples.ebs parsing...");
            Path scriptPath = Path.of("scripts/import_examples.ebs");
            RuntimeContext ctx = Parser.parse(scriptPath);
            System.out.println("✓ Parse successful! Statements: " + ctx.statements.length);
            
            System.out.println("\nTesting util/stringUtil.ebs parsing...");
            Path utilPath = Path.of("scripts/util/stringUtil.ebs");
            RuntimeContext utilCtx = Parser.parse(utilPath);
            System.out.println("✓ Parse successful! Statements: " + utilCtx.statements.length);
            
            System.out.println("\nTesting test dir/subdir/helper.ebs parsing...");
            Path helperPath = Path.of("scripts/test dir/subdir/helper.ebs");
            RuntimeContext helperCtx = Parser.parse(helperPath);
            System.out.println("✓ Parse successful! Statements: " + helperCtx.statements.length);
            
            System.out.println("\n=== All parsing tests passed! ===");
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
