package com.eb.script.test;

import com.eb.script.parser.Parser;
import com.eb.script.RuntimeContext;
import java.nio.file.Path;

public class TestFunctionParse {
    public static void main(String[] args) {
        try {
            System.out.println("Parsing util/stringUtil.ebs...");
            Path utilPath = Path.of("scripts/util/stringUtil.ebs");
            RuntimeContext utilCtx = Parser.parse(utilPath);
            
            System.out.println("Statements: " + utilCtx.statements.length);
            System.out.println("Blocks/Functions: " + (utilCtx.blocks != null ? utilCtx.blocks.size() : 0));
            
            if (utilCtx.blocks != null) {
                System.out.println("Function names:");
                for (String key : utilCtx.blocks.keySet()) {
                    System.out.println("  - " + key);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
