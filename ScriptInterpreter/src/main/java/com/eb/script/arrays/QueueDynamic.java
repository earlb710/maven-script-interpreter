package com.eb.script.arrays;

import com.eb.script.token.DataType;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * A dynamic FIFO queue implementation for the EBS scripting language.
 * Backed by an ArrayDeque for efficient enqueue and dequeue operations.
 *
 * @author Earl Bosch
 */
public class QueueDynamic implements QueueDef<Object> {

    public DataType dataType;
    public ArrayDeque<Object> elements;

    public QueueDynamic(DataType dataType) {
        this.dataType = dataType;
        this.elements = new ArrayDeque<>();
    }

    public QueueDynamic(DataType dataType, int initialCapacity) {
        this.dataType = dataType;
        this.elements = new ArrayDeque<>(initialCapacity);
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public void enqueue(Object value) {
        if (!dataType.isDataType(value)) {
            value = dataType.convertValue(value);
        }
        elements.addLast(value);
    }

    @Override
    public Object dequeue() {
        if (elements.isEmpty()) {
            return null;
        }
        return elements.pollFirst();
    }

    @Override
    public Object peek() {
        if (elements.isEmpty()) {
            return null;
        }
        return elements.peekFirst();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Override
    public boolean contains(Object value) {
        if (!dataType.isDataType(value)) {
            value = dataType.convertValue(value);
        }
        return elements.contains(value);
    }

    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Override
    public Iterator<Object> iterator() {
        return elements.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Queue[");
        boolean first = true;
        for (Object element : elements) {
            if (!first) {
                sb.append(", ");
            }
            if (element instanceof String) {
                sb.append("\"").append(element).append("\"");
            } else {
                sb.append(element);
            }
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

}
