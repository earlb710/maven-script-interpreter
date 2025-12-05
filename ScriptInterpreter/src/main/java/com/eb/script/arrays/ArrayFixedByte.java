package com.eb.script.arrays;

import com.eb.script.token.DataType;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A fixed-size byte array implementation that can represent either BYTE or BITMAP data types.
 * Both BYTE and BITMAP arrays use the same underlying byte[] storage, allowing for 
 * efficient casting between the two types.
 *
 * @author Earl Bosch
 */
public class ArrayFixedByte implements ArrayDef<Byte, byte[]> {

    public DataType dataType;
    public int dimension;
    public byte[] elements;
    private int addIdx = 0;

    public ArrayFixedByte(byte[] elements) {
        this.dataType = DataType.BYTE;
        this.dimension = elements.length;
        this.elements = elements;
    }

    public ArrayFixedByte(int size) {
        this.dataType = DataType.BYTE;
        this.dimension = size;
        elements = new byte[size];
    }
    
    /**
     * Create an ArrayFixedByte with a specific data type (BYTE or BITMAP).
     * @param elements The byte array elements
     * @param dataType The data type (BYTE or BITMAP)
     */
    public ArrayFixedByte(byte[] elements, DataType dataType) {
        if (dataType != DataType.BYTE && dataType != DataType.BITMAP) {
            throw new IllegalArgumentException("ArrayFixedByte only supports BYTE or BITMAP data types");
        }
        this.dataType = dataType;
        this.dimension = elements.length;
        this.elements = elements;
    }

    /**
     * Create an ArrayFixedByte with a specific size and data type (BYTE or BITMAP).
     * @param size The array size
     * @param dataType The data type (BYTE or BITMAP)
     */
    public ArrayFixedByte(int size, DataType dataType) {
        if (dataType != DataType.BYTE && dataType != DataType.BITMAP) {
            throw new IllegalArgumentException("ArrayFixedByte only supports BYTE or BITMAP data types");
        }
        this.dataType = dataType;
        this.dimension = size;
        elements = new byte[size];
    }
    
    /**
     * Create a copy of this array with a different data type (for casting between BYTE and BITMAP).
     * @param targetType The target data type (BYTE or BITMAP)
     * @return A new ArrayFixedByte with the same elements but different data type
     */
    public ArrayFixedByte castTo(DataType targetType) {
        if (targetType != DataType.BYTE && targetType != DataType.BITMAP) {
            throw new IllegalArgumentException("Can only cast ArrayFixedByte to BYTE or BITMAP");
        }
        return new ArrayFixedByte(elements.clone(), targetType);
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
    public Byte get(int index) {
        return elements[index];
    }

    @Override
    public void setElements(Byte... values) {
        int idx = 0;
        for (Byte value : values) {
            if (value != null) {
                elements[idx] = value;
            } else {
                elements[idx] = 0;
            }
            idx++;
        }
    }

    @Override
    public void set(int index, Byte value) {
        if (value == null) {
            elements[index] = 0;
        } else {
            elements[index] = value;
        }
    }

    @Override
    public void add(Byte value) {
        if (value == null) {
            elements[addIdx++] = 0;
        } else {
            elements[addIdx++] = value;
        }
    }

    @Override
    public void add(int idx, Byte value) {
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
    public void fillArray(int length, Byte value) {
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
    public Iterator<Byte> iterator() {
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
    public byte[] getAll() {
        return elements;
    }

    @Override
    public void addAll(ArrayDef<Byte, byte[]> values) {
        byte[] addVal = values.getAll();
        elements = Arrays.copyOf(elements, elements.length + addVal.length);
        System.arraycopy(addVal, 0, elements, elements.length, addVal.length);
    }

    @Override
    public boolean isEmpty() {
        return elements.length == 0;
    }
}
