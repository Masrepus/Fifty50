package com.fifty50.computer;

import java.util.LinkedList;

/**
 * Created by samuel on 14.03.15.
 */
public class Cache<E> extends LinkedList<E> {

    private int limit;

    public Cache(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E e) {
        super.add(e);
        while (size() > limit) super.remove();
        return true;
    }
}
