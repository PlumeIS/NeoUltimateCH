package cn.plumc.ultimatech.utils;

import java.util.LinkedList;

public class FixedLinkedList<T> extends LinkedList<T> {
    private final int capacity;

    public FixedLinkedList(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean add(T t) {
        if (size() + 1 > capacity) removeFirst();
        return super.add(t);
    }
}
