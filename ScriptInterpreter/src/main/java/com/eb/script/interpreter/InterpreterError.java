package com.eb.script.interpreter;

import java.util.Deque;

/**
 *
 * @author Earl Bosch
 */
public class InterpreterError extends Exception {

    public final Deque<Environment.StackInfo> errorStack;

    public InterpreterError(String message, Deque<Environment.StackInfo> stack) {
        super(message);
        this.errorStack = stack;
    }
    
    public InterpreterError(String message) {
        super(message);
        this.errorStack = null;
    }
}
