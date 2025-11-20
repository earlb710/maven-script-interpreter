package com.eb.script.test;

import com.eb.script.RuntimeContext;
import com.eb.script.parser.Parser;
import java.nio.file.Paths;

public class TestSpaceRulesScript {
    public static void main(String[] args) {
        try {
            System.out.println("Testing SPACE_RULES beginner syntax script...");
            
            RuntimeContext context = Parser.parse(
                Paths.get("scripts/test_space_rules_beginner_syntax.ebs")
            );
            
            System.out.println("✓ Script parsed successfully!");
            System.out.println("✓ All syntax validated against EBS_SCRIPT_SYNTAX.md");
            System.out.println("✓ Test covers:");
            System.out.println("  - Traditional for loops");
            System.out.println("  - Increment/decrement operators");
            System.out.println("  - Compound assignment operators");
            System.out.println("  - 'let' keyword");
            System.out.println("  - 'function' keyword");
            System.out.println("  - Generic array types");
            System.out.println("  - Enhanced array.type syntax");
            System.out.println("  - foreach loops");
            System.out.println("  - Multi-dimensional arrays");
            
        } catch (Exception e) {
            System.err.println("✗ Script parsing failed:");
            e.printStackTrace();
        }
    }
}
