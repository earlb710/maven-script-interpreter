package com.eb.script.arrays;

import com.eb.script.token.DataType;
import java.io.Serializable;
import java.util.Iterator;

/**
 *
 * @author Earl Bosch
 */
public interface ArrayDef<E, A> extends Iterable<E>, Serializable {

    public DataType getDataType();

    public A getAll();

    public E get(int index);

    public void setElements(E... values);

    public void set(int index, E value);

    public void add(E value);

    public void add(int idx, E value);

    public void addAll(ArrayDef<E, A> values);

    public void remove(int index);

    public void expandArray(int newLen);

    public void fillArray(int length, E value);

    public void sortArray(boolean ascending);

    public int size();

    public boolean isEmpty();

    public boolean isFixed();
    
    /**
     * Returns an iterator that iterates over the array in reverse order.
     * This enables bidirectional iteration over arrays.
     * 
     * @return an iterator that traverses the array from end to beginning
     */
    public Iterator<E> reverseIterator();

}
