package cn.plumc.ultimatech.utils;

import java.util.Comparator;

public class IntCounter implements Comparator<Integer> {
        private int value;
        public IntCounter() {
            this.value = 0;
        }
        public IntCounter(int value) {
            this.value = value;
        }
        public void add(){
            value += 1;
        }
        public int add(int value) {
            return this.value += value;
        }
        public int get() {
            return value;
        }
        public void set(int value) {
            this.value = value;
        }

    @Override
    public int compare(Integer o1, Integer o2) {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}