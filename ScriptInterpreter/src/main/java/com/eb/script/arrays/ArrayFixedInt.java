package com.eb.script.arrays;

import com.eb.script.token.DataType;
import java.util.Arrays;
import java.util.Iterator;

/**
 *
 * @author Earl Bosch
 */
public class ArrayFixedInt implements ArrayDef<Integer, int[]> {

    public DataType dataType;
    public int dimension;
    public int[] elements;
    private int addIdx = 0;

    public ArrayFixedInt(int[] elements) {
        this.dataType = DataType.INTEGER;
        this.dimension = elements.length;
        this.elements = elements;
    }

    public ArrayFixedInt(int size) {
        this.dataType = DataType.INTEGER;
        this.dimension = size;
        elements = new int[size];
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
    public Integer get(int index) {
        return elements[index];
    }

    @Override
    public void setElements(Integer... values) {
        int idx = 0;
        for (Integer value : values) {
            if (value != null) {
                elements[idx] = value;
            } else {
                elements[idx] = 0;
            }
            idx++;
        }
    }

    @Override
    public void set(int index, Integer value) {
        if (value == null) {
            elements[index] = 0;
        } else {
            elements[index] = value;
        }
    }

    @Override
    public void add(Integer value) {
        if (value == null) {
            elements[addIdx++] = 0;
        } else {
            elements[addIdx++] = value;
        }
    }

    @Override
    public void add(int idx, Integer value) {
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
    public void fillArray(int length, Integer value) {
        Arrays.fill(elements, value);
    }

    @Override
    public void sortArray(boolean ascending) {
        if (dataType != DataType.JSON) {
            if (ascending) {
                Arrays.sort(elements);
            } else {
                Arrays.sort(elements);
            }
        }
    }

    @Override
    public Iterator<Integer> iterator() {
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
    public int[] getAll() {
        return elements;
    }

    @Override
    public void addAll(ArrayDef<Integer, int[]> values) {
        int[] addVal = values.getAll();
        elements = Arrays.copyOf(elements, elements.length + addVal.length);
        System.arraycopy(addVal, 0, elements, elements.length, addVal.length);
    }

    @Override
    public boolean isEmpty() {
        return elements.length == 0;
    }
}
