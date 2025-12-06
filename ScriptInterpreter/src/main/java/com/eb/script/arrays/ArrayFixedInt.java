package com.eb.script.arrays;

import com.eb.script.token.DataType;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A fixed-size int array implementation that can represent either INTEGER or INTMAP data types.
 * Both INTEGER and INTMAP arrays use the same underlying int[] storage, allowing for 
 * efficient casting between the two types.
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
    
    /**
     * Create an ArrayFixedInt with a specific data type (INTEGER or INTMAP).
     * @param elements The int array elements
     * @param dataType The data type (INTEGER or INTMAP)
     */
    public ArrayFixedInt(int[] elements, DataType dataType) {
        if (dataType != DataType.INTEGER && dataType != DataType.INTMAP) {
            throw new IllegalArgumentException("ArrayFixedInt only supports INTEGER or INTMAP data types");
        }
        this.dataType = dataType;
        this.dimension = elements.length;
        this.elements = elements;
    }

    /**
     * Create an ArrayFixedInt with a specific size and data type (INTEGER or INTMAP).
     * @param size The array size
     * @param dataType The data type (INTEGER or INTMAP)
     */
    public ArrayFixedInt(int size, DataType dataType) {
        if (dataType != DataType.INTEGER && dataType != DataType.INTMAP) {
            throw new IllegalArgumentException("ArrayFixedInt only supports INTEGER or INTMAP data types");
        }
        this.dataType = dataType;
        this.dimension = size;
        elements = new int[size];
    }
    
    /**
     * Create a copy of this array with a different data type (for casting between INTEGER and INTMAP).
     * @param targetType The target data type (INTEGER or INTMAP)
     * @return A new ArrayFixedInt with the same elements but different data type
     */
    public ArrayFixedInt castTo(DataType targetType) {
        if (targetType != DataType.INTEGER && targetType != DataType.INTMAP) {
            throw new IllegalArgumentException("Can only cast ArrayFixedInt to INTEGER or INTMAP");
        }
        return new ArrayFixedInt(elements.clone(), targetType);
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
