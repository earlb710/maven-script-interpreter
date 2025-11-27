package com.eb.script.arrays;

import com.eb.script.token.DataType;

/**
 * Interface for queue data structures in the EBS scripting language.
 * Provides FIFO (First-In-First-Out) queue operations.
 *
 * @author Earl Bosch
 */
public interface QueueDef<E> extends Iterable<E> {

    /**
     * Get the data type of elements in this queue.
     */
    DataType getDataType();

    /**
     * Add an element to the back of the queue (enqueue).
     */
    void enqueue(E value);

    /**
     * Remove and return the element at the front of the queue (dequeue).
     * Returns null if the queue is empty.
     */
    E dequeue();

    /**
     * Return the element at the front of the queue without removing it (peek).
     * Returns null if the queue is empty.
     */
    E peek();

    /**
     * Return the number of elements in the queue.
     */
    int size();

    /**
     * Check if the queue is empty.
     */
    boolean isEmpty();

    /**
     * Remove all elements from the queue.
     */
    void clear();

    /**
     * Check if the queue contains a specific element.
     */
    boolean contains(E value);

    /**
     * Convert the queue contents to an array.
     */
    Object[] toArray();

}
