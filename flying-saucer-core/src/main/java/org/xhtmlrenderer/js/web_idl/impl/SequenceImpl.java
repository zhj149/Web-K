package org.xhtmlrenderer.js.web_idl.impl;

import org.xhtmlrenderer.js.web_idl.Sequence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Taras Maslov
 * 7/27/2018
 */
public class SequenceImpl<T> implements Sequence<T> {
    
    List<T> items = new ArrayList<>();

    public SequenceImpl(List<T> items) {
        this.items = items;
    }

    @Override
    public T item(int index) {
        return items.size() > index ? items.get(index) : null;
    }

    @Override
    public int length() {
        return items.size();
    }
}