package com.eb.script.arrays;

import com.eb.script.token.DataType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Earl Bosch
 */
public class ArrayDynamic implements ArrayDef<Object, List<Object>> {

    public DataType dataType;
    public Integer dimension;
    public ArrayList<Object> elements;

    public ArrayDynamic(DataType dataType) {
        this.dataType = dataType;
        this.dimension = 0;
        elements = new ArrayList();
    }

    public ArrayDynamic(DataType dataType, Integer size) {
        this.dataType = dataType;
        this.dimension = size;
        elements = new ArrayList();
        Object[] e = dataType.getArray(size);
        Collections.addAll(elements, e);
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public boolean isFixed() {
        return false;
    }

    @Override
    public Object get(int index) {
        return elements.get(index);
    }

    @Override
    public void set(int index, Object value) {
        if (!dataType.isDataType(value)) {
            value = dataType.convertValue(value);
        }
        if (index == elements.size()) {
            elements.add(value);
        } else {
            elements.set(index, value);
        }
    }

    @Override
    public void setElements(Object... values) {
        int idx = 0;
        for (Object value : values) {
            if (!dataType.isDataType(value)) {
                value = dataType.convertValue(value);
            }
            if (idx == elements.size()) {
                elements.add(value);
            } else {
                elements.set(idx, value);
            }
            idx++;
        }
    }

    @Override
    public void add(Object value) {
        if (!dataType.isDataType(value)) {
            value = dataType.convertValue(value);
        }
        elements.add(value);
    }

    @Override
    public void add(int idx, Object value) {
        if (!dataType.isDataType(value)) {
            value = dataType.convertValue(value);
        }
        elements.add(idx, value);
    }

    @Override
    public void remove(int index) {
        elements.remove(index);
    }

    @Override
    public void expandArray(int newLen) {
        if (elements.size() < newLen) {
            for (int idx = elements.size(); idx < newLen; idx++) {
                elements.add(null);
            }
        }
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public void fillArray(int length, Object value) {
        Collections.fill(elements, value);
    }

    @Override
    public void sortArray(boolean ascending) {
        if (dataType != DataType.JSON) {
            if (ascending) {
                Collections.sort((ArrayList) elements);
            } else {
                Collections.sort(elements, Collections.reverseOrder());
            }
        }
    }

    @Override
    public Iterator<Object> iterator() {
        return elements.iterator();
    }

    @Override
    public List<Object> getAll() {
        return elements;
    }

    @Override
    public void addAll(ArrayDef<Object, List<Object>> values) {
        elements.addAll(values.getAll());
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

}
