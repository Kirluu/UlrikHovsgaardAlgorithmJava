package com.dcr.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ComparableList<T> extends ArrayList<T> {
    public ComparableList() {
        super();
    }

    public ComparableList(List<T> items) {
        super(items);
    }

    @Override
    public boolean equals(Object obj) {
        ComparableList<T> otherList = (ComparableList<T>)obj;
        if (otherList == null || this.size() != otherList.size())
        {
            return false;
        }
        return IntStream.range(0, this.size()).allMatch(i -> this.get(i).equals(otherList.get(i)));
    }

    @Override
    public int hashCode() {
        int res = 17;
        for (T item : this) {
            res = res * 31 + item.hashCode();
        }
        return res;
        //return this.stream().foldLeft(0, (aggr, item) => aggr * 31 + item.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (T item : this) {
            res.append(", ").append(item.toString());
        }
        return res.toString();
        //return this.stream().foldLeft("", (current, elem) => current + (elem.ToString()));
    }
}
