package com.eb.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utility class to analyze and display the class hierarchy tree structure
 * of the Java source files in the project.
 */
public class ClassTreeLister {
    
    private static class ClassInfo {
        String packageName;
        String className;
        String fullClassName;
        String extendsClass;
        List<String> implementsInterfaces = new ArrayList<>();
        boolean isInterface;
        boolean isAbstract;
        boolean isEnum;
        
        @Override
        public String toString() {
            return fullClassName;
        }
    }
    
    private Map<String, ClassInfo> classMap = new HashMap<>();
    private Map<String, List<String>> childrenMap = new HashMap<>();
    
    /**
     * Scans the source directory for Java files and builds the class hierarchy
     */
    public void scan(String sourceDir) throws IOException {
        Path startPath = Paths.get(sourceDir);
        
        try (Stream<Path> paths = Files.walk(startPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(this::parseJavaFile);
        }
        
        buildHierarchy();
    }
    
    /**
     * Parses a Java file and extracts class information
     */
    private void parseJavaFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            ClassInfo info = extractClassInfo(content);
            if (info != null) {
                classMap.put(info.fullClassName, info);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath);
        }
    }
    
    /**
     * Extracts class information from Java source code
     */
    private ClassInfo extractClassInfo(String content) {
        ClassInfo info = new ClassInfo();
        
        // Remove comments to avoid false matches
        content = removeComments(content);
        
        // Extract package
        Pattern packagePattern = Pattern.compile("package\\s+([\\w.]+)\\s*;");
        Matcher packageMatcher = packagePattern.matcher(content);
        if (packageMatcher.find()) {
            info.packageName = packageMatcher.group(1);
        }
        
        // Extract class/interface/enum declaration - more precise pattern
        Pattern classPattern = Pattern.compile(
            "(?:^|\\n)\\s*(?:public|private|protected)?\\s*(?:abstract|final|static)?\\s*" +
            "(class|interface|enum)\\s+(\\w+)" +
            "(?:<[^>]+>)?\\s*" +  // Optional generics in class declaration
            "(?:extends\\s+([\\w.]+)(?:<[^>]+>)?)?\\s*" +  // Optional extends with generics
            "(?:implements\\s+([^{]+))?\\s*" +  // Optional implements
            "\\{"
        );
        Matcher classMatcher = classPattern.matcher(content);
        
        if (classMatcher.find()) {
            String type = classMatcher.group(1);
            info.className = classMatcher.group(2);
            info.fullClassName = info.packageName != null ? 
                info.packageName + "." + info.className : info.className;
            
            info.isInterface = "interface".equals(type);
            info.isEnum = "enum".equals(type);
            
            // Check if abstract by looking at the actual line
            int start = Math.max(0, classMatcher.start() - 100);
            int end = Math.min(content.length(), classMatcher.start() + 100);
            String declaration = content.substring(start, end);
            info.isAbstract = declaration.contains("abstract class");
            
            String extendsClause = classMatcher.group(3);
            if (extendsClause != null) {
                info.extendsClass = resolveClassName(extendsClause.trim(), info.packageName, content);
            }
            
            String implementsClause = classMatcher.group(4);
            if (implementsClause != null) {
                // Parse implements clause carefully, handling generics
                // Split by commas that are not inside angle brackets
                List<String> interfaces = splitByCommaOutsideGenerics(implementsClause);
                for (String iface : interfaces) {
                    // Extract interface name before any generics
                    String ifaceName = iface.trim().split("[<\\s]")[0].trim();
                    if (!ifaceName.isEmpty()) {
                        String resolved = resolveClassName(ifaceName, info.packageName, content);
                        info.implementsInterfaces.add(resolved);
                    }
                }
            }
            
            return info;
        }
        
        return null;
    }
    
    /**
     * Removes comments from Java source code
     */
    private String removeComments(String content) {
        // Remove single-line comments
        content = content.replaceAll("//.*?\\n", "\n");
        // Remove multi-line comments (non-greedy)
        content = content.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");
        return content;
    }
    
    /**
     * Splits a string by commas that are outside of angle brackets (generics)
     */
    private List<String> splitByCommaOutsideGenerics(String input) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        
        for (char c : input.toCharArray()) {
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                result.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
    
    /**
     * Resolves a class name to its fully qualified name
     */
    private String resolveClassName(String className, String currentPackage, String content) {
        className = className.trim();
        
        // If already fully qualified
        if (className.contains(".")) {
            return className;
        }
        
        // Check for import statements - escape special regex characters in className
        String escapedClassName = Pattern.quote(className);
        Pattern importPattern = Pattern.compile("import\\s+([\\w.]+\\." + escapedClassName + ")\\s*;");
        Matcher importMatcher = importPattern.matcher(content);
        if (importMatcher.find()) {
            return importMatcher.group(1);
        }
        
        // Check if it's in java.lang or current package
        if (isJavaLangClass(className)) {
            return "java.lang." + className;
        }
        
        // Assume it's in the current package
        return currentPackage != null ? currentPackage + "." + className : className;
    }
    
    /**
     * Checks if a class is in java.lang package
     */
    private boolean isJavaLangClass(String className) {
        Set<String> javaLangClasses = Set.of(
            "Object", "String", "Integer", "Long", "Double", "Float",
            "Boolean", "Character", "Byte", "Short", "Exception", "Throwable"
        );
        return javaLangClasses.contains(className);
    }
    
    /**
     * Builds the parent-child hierarchy map
     */
    private void buildHierarchy() {
        for (ClassInfo info : classMap.values()) {
            // Add extends relationship
            if (info.extendsClass != null) {
                childrenMap.computeIfAbsent(info.extendsClass, k -> new ArrayList<>())
                          .add(info.fullClassName);
            }
            
            // Add implements relationships
            for (String iface : info.implementsInterfaces) {
                childrenMap.computeIfAbsent(iface, k -> new ArrayList<>())
                          .add(info.fullClassName);
            }
        }
    }
    
    /**
     * Prints the class hierarchy tree
     */
    public void printTree() {
        System.out.println("=".repeat(80));
        System.out.println("CLASS HIERARCHY TREE");
        System.out.println("=".repeat(80));
        System.out.println();
        
        // Find root classes (those that don't extend anything in our codebase)
        Set<String> printedClasses = new HashSet<>();
        
        // Print by package
        Map<String, List<ClassInfo>> packageMap = new TreeMap<>();
        for (ClassInfo info : classMap.values()) {
            String pkg = info.packageName != null ? info.packageName : "(default)";
            packageMap.computeIfAbsent(pkg, k -> new ArrayList<>()).add(info);
        }
        
        for (Map.Entry<String, List<ClassInfo>> entry : packageMap.entrySet()) {
            System.out.println("Package: " + entry.getKey());
            System.out.println("-".repeat(80));
            
            // Sort classes within package
            List<ClassInfo> classes = entry.getValue();
            classes.sort(Comparator.comparing(c -> c.className));
            
            for (ClassInfo info : classes) {
                if (!printedClasses.contains(info.fullClassName)) {
                    printClass(info, 0, printedClasses);
                }
            }
            System.out.println();
        }
        
        // Print statistics
        System.out.println("=".repeat(80));
        System.out.println("STATISTICS");
        System.out.println("=".repeat(80));
        System.out.println("Total classes: " + classMap.values().stream()
                .filter(c -> !c.isInterface && !c.isEnum).count());
        System.out.println("Total interfaces: " + classMap.values().stream()
                .filter(c -> c.isInterface).count());
        System.out.println("Total enums: " + classMap.values().stream()
                .filter(c -> c.isEnum).count());
        System.out.println("Abstract classes: " + classMap.values().stream()
                .filter(c -> c.isAbstract).count());
    }
    
    /**
     * Prints a class and its children recursively
     */
    private void printClass(ClassInfo info, int depth, Set<String> printed) {
        if (printed.contains(info.fullClassName)) {
            return;
        }
        
        printed.add(info.fullClassName);
        
        String indent = "  ".repeat(depth);
        String prefix = depth > 0 ? "└─ " : "";
        
        StringBuilder line = new StringBuilder(indent + prefix);
        
        // Add type indicator
        if (info.isInterface) {
            line.append("«interface» ");
        } else if (info.isEnum) {
            line.append("«enum» ");
        } else if (info.isAbstract) {
            line.append("«abstract» ");
        }
        
        line.append(info.className);
        
        // Add extends/implements info
        List<String> relationships = new ArrayList<>();
        if (info.extendsClass != null) {
            String shortName = getShortName(info.extendsClass);
            relationships.add("extends " + shortName);
        }
        if (!info.implementsInterfaces.isEmpty()) {
            String interfaces = String.join(", ", 
                info.implementsInterfaces.stream()
                    .map(this::getShortName)
                    .toArray(String[]::new));
            relationships.add("implements " + interfaces);
        }
        
        if (!relationships.isEmpty()) {
            line.append(" (").append(String.join(", ", relationships)).append(")");
        }
        
        System.out.println(line);
        
        // Print children
        List<String> children = childrenMap.get(info.fullClassName);
        if (children != null) {
            for (String childName : children) {
                ClassInfo child = classMap.get(childName);
                if (child != null) {
                    printClass(child, depth + 1, printed);
                }
            }
        }
    }
    
    /**
     * Gets the short name of a class (without package)
     */
    private String getShortName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }
    
    /**
     * Main method to run the class tree lister
     */
    public static void main(String[] args) {
        try {
            String sourceDir = args.length > 0 ? args[0] : "src/main/java";
            
            ClassTreeLister lister = new ClassTreeLister();
            System.out.println("Scanning directory: " + new File(sourceDir).getAbsolutePath());
            System.out.println();
            
            lister.scan(sourceDir);
            lister.printTree();
            
        } catch (IOException e) {
            System.err.println("Error scanning files: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
