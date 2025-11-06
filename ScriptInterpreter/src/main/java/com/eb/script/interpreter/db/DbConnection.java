package com.eb.script.interpreter.db;

/**
 *
 * @author Earl Bosch
 * 
 */

public interface DbConnection {
    DbCursor openCursor(String sql,
                        java.util.Map<String,Object> named,
                        java.util.List<Object> positional) throws Exception;

    java.util.List<java.util.Map<String,Object>> executeSelect(String sql,
                        java.util.Map<String,Object> named,
                        java.util.List<Object> positional) throws Exception;

    void close() throws Exception;
}
