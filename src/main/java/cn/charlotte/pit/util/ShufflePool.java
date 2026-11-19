package cn.charlotte.pit.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ShufflePool<T> {
    private final List<T> source;
    private final Random random = new Random();
    private List<T> pool;
    private int index = 0;
    private T lastPicked = null;

    public ShufflePool(List<T> source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("Source list cannot be empty!");
        }
        this.source = new ArrayList<>(source);
        reshuffle();
    }

    private void reshuffle() {
        if (pool == null) {
            pool = new ArrayList<>(source);
        }
        Collections.shuffle(pool, random);
        index = 0;
    }

    public T next() {
        if (pool.isEmpty()) {
            return null;
        }

        if (pool.size() == 1) {
            return pool.getFirst();
        }

        T picked;
        int attempts = 0;

        do {
            if (index >= pool.size()) {
                reshuffle();
            }
            picked = pool.get(index++);
            attempts++;
        } while (picked.equals(lastPicked) && attempts < 5); // 避免连续重复

        lastPicked = picked;
        return picked;
    }
}