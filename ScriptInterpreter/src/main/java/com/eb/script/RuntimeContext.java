package com.eb.script;

import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.statement.BlockStatement;
import com.eb.script.interpreter.statement.Statement;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
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
        this.blocks = new HashMap();
        this.statements = null;
    }

    public RuntimeContext(String name, Map<String, BlockStatement> blocks, Statement[] statements) {
        this(name, null, blocks, statements);
    }

    public RuntimeContext(String name, Path sourcePath, Map<String, BlockStatement> blocks, Statement[] statements) {
        this.name = name;
        this.sourcePath = sourcePath;
        this.blocks = blocks;
        this.statements = statements;
    }

}
