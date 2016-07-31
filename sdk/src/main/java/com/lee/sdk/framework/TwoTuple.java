package com.lee.sdk.framework;

/**
 * @author jiangli
 */
public class TwoTuple<R, T> {
    private R r;
    private T t;

    public TwoTuple() {

    }

    public TwoTuple(R r, T t) {
        this.r = r;
        this.t = t;
    }

    public R getFirst() {
        return this.r;
    }

    public void setFirst(R r) {
        this.r = r;
    }

    public T getSecond() {
        return this.t;
    }

    public void setSecond(T t) {
        this.t = t;
    }
}
