package com.eb.script.arrays;

import com.eb.script.token.DataType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 *
 * @author Earl Bosch
 */
public class ArrayFixed implements ArrayDef<Object, Object[]> {

    public DataType dataType;
    public int dimension;
    public Object[] elements;
    private int addIdx = 0;

    public ArrayFixed(DataType dataType, Object[] elements) {
        this.dataType = dataType;
        this.dimension = elements.length;
        this.elements = elements;
    }

    public ArrayFixed(DataType dataType, int size) {
        this.dataType = dataType;
        this.dimension = size;
        elements = new Object[size];
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public boolean isFixed() {
        return true;
    }

    @Override
    public Object get(int index) {
        return elements[index];
    }

    @Override
    public void setElements(Object... values) {
        int idx = 0;
        for (Object value : values) {
            // Skip type conversion for nested arrays (ArrayDef objects)
            if (!(value instanceof ArrayDef) && !dataType.isDataType(value)) {
                value = dataType.convertValue(value);
            }
            elements[idx] = value;
            idx++;
        }
    }

    @Override
    public void set(int index, Object value) {
        // Skip type conversion for nested arrays (ArrayDef objects)
        // These are stored as-is in multi-dimensional arrays
        if (!(value instanceof ArrayDef) && !dataType.isDataType(value)) {
            value = dataType.convertValue(value);
        }
        elements[index] = value;
    }

    @Override
    public void add(Object value) {
        // Skip type conversion for nested arrays (ArrayDef objects)
        if (!(value instanceof ArrayDef) && !dataType.isDataType(value)) {
            value = dataType.convertValue(value);
        }
        elements[addIdx++] = value;
    }

    @Override
    public void add(int idx, Object value) {
        // Skip type conversion for nested arrays (ArrayDef objects)
        if (!(value instanceof ArrayDef) && !dataType.isDataType(value)) {
            value = dataType.convertValue(value);
        }
        elements[idx] = value;
    }

    @Override
    public void remove(int index) {
    }

    @Override
    public void expandArray(int newLen) {
        if (elements.length < newLen) {
            elements = Arrays.copyOf(elements, newLen);
        }
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public void fillArray(int length, Object value) {
        Arrays.fill(elements, value);
    }

    @Override
    public void sortArray(boolean ascending) {
        if (dataType != DataType.JSON) {
            if (ascending) {
                Arrays.sort(elements);
            } else {
                Arrays.sort(elements, Collections.reverseOrder());
            }
        }
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator() {
            int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < elements.length;
            }

            @Override
            public Object next() {
                return elements[idx++];
            }

        };
    }

    @Override
    public Iterator<Object> reverseIterator() {
        return new Iterator<Object>() {
            int idx = elements.length - 1;

            @Override
            public boolean hasNext() {
                return idx >= 0;
            }

            @Override
            public Object next() {
                return elements[idx--];
            }
        };
    }

    @Override
    public Object[] getAll() {
        return elements;
    }

    @Override
    public void addAll(ArrayDef<Object, Object[]> values) {
        Object[] addVal = values.getAll();
        elements = Arrays.copyOf(elements, elements.length + addVal.length);
        System.arraycopy(addVal, 0, elements, elements.length, addVal.length);
    }

    @Override
    public boolean isEmpty() {
        return elements.length == 0;
    }
}
