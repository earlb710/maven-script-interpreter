package com.eb.script.interpreter.builtins;

import com.eb.script.arrays.ArrayDynamic;
import com.eb.script.arrays.QueueDef;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.token.DataType;

/**
 * Built-in functions for Queue operations.
 * Handles queue.* builtins for FIFO queue data structure.
 *
 * @author Earl Bosch
 */
public class BuiltinsQueue {

    /**
     * Dispatch a Queue builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "queue.enqueue")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "queue.enqueue" -> enqueue(args);
            case "queue.dequeue" -> dequeue(args);
            case "queue.peek" -> peek(args);
            case "queue.isempty" -> isEmpty(args);
            case "queue.size" -> size(args);
            case "queue.clear" -> clear(args);
            case "queue.contains" -> contains(args);
            case "queue.toarray" -> toArray(args);
            default -> throw new InterpreterError("Unknown Queue builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Queue builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("queue.");
    }

    // --- Individual builtin implementations ---

    /**
     * queue.enqueue(queue, value) - Add an element to the back of the queue
     */
    @SuppressWarnings("unchecked")
    private static Object enqueue(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("queue.enqueue: queue cannot be null");
        }
        if (!(args[0] instanceof QueueDef)) {
            throw new InterpreterError("queue.enqueue: first argument must be a queue, got: " + args[0].getClass().getSimpleName());
        }
        QueueDef<Object> queue = (QueueDef<Object>) args[0];
        Object value = args[1];
        queue.enqueue(value);
        return null;
    }

    /**
     * queue.dequeue(queue) - Remove and return the element at the front of the queue
     */
    @SuppressWarnings("unchecked")
    private static Object dequeue(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("queue.dequeue: queue cannot be null");
        }
        if (!(args[0] instanceof QueueDef)) {
            throw new InterpreterError("queue.dequeue: first argument must be a queue, got: " + args[0].getClass().getSimpleName());
        }
        QueueDef<Object> queue = (QueueDef<Object>) args[0];
        return queue.dequeue();
    }

    /**
     * queue.peek(queue) - Return the element at the front without removing it
     */
    @SuppressWarnings("unchecked")
    private static Object peek(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("queue.peek: queue cannot be null");
        }
        if (!(args[0] instanceof QueueDef)) {
            throw new InterpreterError("queue.peek: first argument must be a queue, got: " + args[0].getClass().getSimpleName());
        }
        QueueDef<Object> queue = (QueueDef<Object>) args[0];
        return queue.peek();
    }

    /**
     * queue.isEmpty(queue) - Check if the queue is empty
     */
    @SuppressWarnings("unchecked")
    private static Object isEmpty(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("queue.isEmpty: queue cannot be null");
        }
        if (!(args[0] instanceof QueueDef)) {
            throw new InterpreterError("queue.isEmpty: first argument must be a queue, got: " + args[0].getClass().getSimpleName());
        }
        QueueDef<Object> queue = (QueueDef<Object>) args[0];
        return queue.isEmpty();
    }

    /**
     * queue.size(queue) - Return the number of elements in the queue
     */
    @SuppressWarnings("unchecked")
    private static Object size(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("queue.size: queue cannot be null");
        }
        if (!(args[0] instanceof QueueDef)) {
            throw new InterpreterError("queue.size: first argument must be a queue, got: " + args[0].getClass().getSimpleName());
        }
        QueueDef<Object> queue = (QueueDef<Object>) args[0];
        return queue.size();
    }

    /**
     * queue.clear(queue) - Remove all elements from the queue
     */
    @SuppressWarnings("unchecked")
    private static Object clear(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("queue.clear: queue cannot be null");
        }
        if (!(args[0] instanceof QueueDef)) {
            throw new InterpreterError("queue.clear: first argument must be a queue, got: " + args[0].getClass().getSimpleName());
        }
        QueueDef<Object> queue = (QueueDef<Object>) args[0];
        queue.clear();
        return null;
    }

    /**
     * queue.contains(queue, value) - Check if the queue contains a specific element
     */
    @SuppressWarnings("unchecked")
    private static Object contains(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("queue.contains: queue cannot be null");
        }
        if (!(args[0] instanceof QueueDef)) {
            throw new InterpreterError("queue.contains: first argument must be a queue, got: " + args[0].getClass().getSimpleName());
        }
        QueueDef<Object> queue = (QueueDef<Object>) args[0];
        Object value = args[1];
        return queue.contains(value);
    }

    /**
     * queue.toArray(queue) - Convert the queue to an array
     */
    @SuppressWarnings("unchecked")
    private static Object toArray(Object[] args) throws InterpreterError {
        if (args[0] == null) {
            throw new InterpreterError("queue.toArray: queue cannot be null");
        }
        if (!(args[0] instanceof QueueDef)) {
            throw new InterpreterError("queue.toArray: first argument must be a queue, got: " + args[0].getClass().getSimpleName());
        }
        QueueDef<Object> queue = (QueueDef<Object>) args[0];
        
        // Create a new ArrayDynamic with the queue's data type and fill it with queue elements
        ArrayDynamic array = new ArrayDynamic(queue.getDataType());
        for (Object element : queue) {
            array.add(element);
        }
        return array;
    }
}
