package com.eb.script.arrays;

import com.eb.script.token.DataType;
import java.util.Arrays;
import java.util.Iterator;

/**
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
        elements[index] = value;
    }

    @Override
    public void add(Byte value) {
        elements[addIdx++] = value;
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
