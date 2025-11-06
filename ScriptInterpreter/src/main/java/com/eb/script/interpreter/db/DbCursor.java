package com.eb.script.interpreter.db;

/**
 *
 * @author Earl Bosch
 */

public interface DbCursor {
    boolean hasNext() throws Exception;
    java.util.Map<String,Object> next() throws Exception;
    void close() throws Exception;
}
