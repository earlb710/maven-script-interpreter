package com.eb.script;

import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.statement.BlockStatement;
import com.eb.script.interpreter.statement.Statement;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RuntimeContext holds the parsed script context including functions (blocks),
 * statements, and environment. Uses thread-safe collections to allow screen
 * threads to access functions defined in the main script.
 *
 * @author Earl Bosch
 */
public class RuntimeContext {

    public final String name;
    public final Path sourcePath;  // Path to the source file, if loaded from a file
    public final Environment environment = new Environment();
    public Map<String, BlockStatement> blocks;
    public Statement[] statements;

    public RuntimeContext(String name) {
        this(name, null);
    }

    public RuntimeContext(String name, Path sourcePath) {
        this.name = name;
        this.sourcePath = sourcePath;
        this.blocks = new ConcurrentHashMap<>();
        this.statements = null;
    }

    public RuntimeContext(String name, Map<String, BlockStatement> blocks, Statement[] statements) {
        this(name, null, blocks, statements);
    }

    public RuntimeContext(String name, Path sourcePath, Map<String, BlockStatement> blocks, Statement[] statements) {
        this.name = name;
        this.sourcePath = sourcePath;
        // Use ConcurrentHashMap for thread-safe access from screen threads
        this.blocks = blocks != null ? new ConcurrentHashMap<>(blocks) : new ConcurrentHashMap<>();
        this.statements = statements;
    }

}
