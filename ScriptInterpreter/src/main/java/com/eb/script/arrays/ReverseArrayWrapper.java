package com.eb.script.arrays;

import java.util.Iterator;

/**
 * A wrapper that provides reverse iteration over an ArrayDef.
 * This enables bidirectional iteration by returning elements in reverse order.
 * 
 * @author Earl Bosch
 */
public class ReverseArrayWrapper<E> implements Iterable<E> {
    
    private final ArrayDef<E, ?> array;
    
    public ReverseArrayWrapper(ArrayDef<E, ?> array) {
        this.array = array;
    }
    
    @Override
    public Iterator<E> iterator() {
        return array.reverseIterator();
    }
    
    /**
     * Get the underlying array
     */
    public ArrayDef<E, ?> getArray() {
        return array;
    }
}
