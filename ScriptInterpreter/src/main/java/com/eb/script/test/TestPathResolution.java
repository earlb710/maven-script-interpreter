package com.eb.script.test;

import java.nio.file.Path;
import java.nio.file.Files;

public class TestPathResolution {
    public static void main(String[] args) {
        // Test 1: Simple subdirectory
        Path scriptDir = Path.of("scripts");
        Path resolved1 = scriptDir.resolve("util/stringUtil.ebs");
        System.out.println("Test 1: Resolve 'util/stringUtil.ebs'");
        System.out.println("  Result: " + resolved1);
        System.out.println("  Exists: " + Files.exists(resolved1));
        System.out.println();
        
        // Test 2: Subdirectory with spaces
        Path resolved2 = scriptDir.resolve("test dir/subdir/helper.ebs");
        System.out.println("Test 2: Resolve 'test dir/subdir/helper.ebs'");
        System.out.println("  Result: " + resolved2);
        System.out.println("  Exists: " + Files.exists(resolved2));
        System.out.println();
        
        // Test 3: Forward slashes are handled correctly by Path.resolve
        Path resolved3 = scriptDir.resolve("a/b/c.ebs");
        System.out.println("Test 3: Resolve 'a/b/c.ebs'");
        System.out.println("  Result: " + resolved3);
        System.out.println("  Platform-specific path: " + resolved3.toString());
    }
}
