package cn.charlotte.pit.util;

import java.util.Map;

public class Pair<A, B> {

    private A key;
    private B value;

    public Pair(A key, B value) {
        this.key = key;
        this.value = value;
    }

    public A key() {
        return this.key;
    }

    public B value() {
        return this.value;
    }

    public void key(A key) {
        this.key = key;
    }

    public void value(B value) {
        this.value = value;
    }

    public Map<A, B> toMap() {
        return Map.of(this.key, this.value);
    }
}
