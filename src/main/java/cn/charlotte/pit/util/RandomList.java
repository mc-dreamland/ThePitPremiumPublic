package cn.charlotte.pit.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

public class RandomList<T> {
    private final TreeMap<Integer, T> weightMap = new TreeMap<>();
    private int totalWeight = 0;

    @SafeVarargs
    public RandomList(Map.Entry<T, Integer>... elements) {
        Arrays.stream(elements).forEach(pair -> add(pair.getKey(), pair.getValue()));
    }

    public RandomList(Collection<Map.Entry<T, Integer>> elements) {
        elements.forEach(pair -> add(pair.getKey(), pair.getValue()));
    }

    /**
     * 转换为随机列表
     */
    public static <T> RandomList<T> toRandomList(Collection<Map.Entry<T, Integer>> elements) {
        return new RandomList<>(elements);
    }

    /**
     * 获取随机元素
     */
    public T random() {
        if (totalWeight <= 0) return null;

        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        Map.Entry<Integer, T> entry = weightMap.ceilingEntry(randomWeight);
        return entry.getValue();
    }

    /**
     * 添加元素
     */
    public void add(T element, int weight) {
        if (weight <= 0) return;
        totalWeight += weight;
        weightMap.put(totalWeight, element);
    }

    /**
     * 批量添加元素
     */
    public void addAll(Collection<Map.Entry<T, Integer>> elements) {
        elements.forEach(pair -> add(pair.getKey(), pair.getValue()));
    }

    /**
     * 获取所有元素
     */
    public Collection<T> values() {
        return weightMap.values();
    }

    /**
     * 获取元素数量
     */
    public int size() {
        return weightMap.size();
    }

    /**
     * 清空元素
     */
    public void clear() {
        weightMap.clear();
    }
}
