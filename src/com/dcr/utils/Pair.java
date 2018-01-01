package com.dcr.utils;

public class Pair<S, T> {
    private final S x;
    private final T y;

    public S getX() { return x; }
    public T getY() { return y; }

    public Pair(S x, T y) {
        this.x = x;
        this.y = y;
    }
}
