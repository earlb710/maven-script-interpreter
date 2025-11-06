package com.eb.script.interpreter.db;

/**
 *
 * @author Earl Bosch
 */

public interface DbAdapter {
    DbConnection connect(Object spec) throws Exception;

    DbAdapter NOOP = new DbAdapter() {
        @Override public DbConnection connect(Object spec) {
            throw new UnsupportedOperationException("No DbAdapter configured for database operations");
        }
    };
}
