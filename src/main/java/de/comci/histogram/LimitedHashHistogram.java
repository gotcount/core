/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class LimitedHashHistogram<T> extends HashHistogram<T> {

    private final int limit;

    public LimitedHashHistogram() {
        this(0);
    }

    public LimitedHashHistogram(int limit) {
        this.limit = limit;
    }

    @Override
    public void set(T t, int count) {

        if (limit != 0 && size() >= Math.abs(limit)) {
            final T last = keySet(limit < 0).last();
            final Integer lastValue = get(last);
            if ((limit > 0 && count > lastValue)
                    || (limit < 0 && count < lastValue)) {
                remove(last);
            }
        }

        super.set(t, count);

    }
    
    @Override
    public String toString() {
        return entrySet(limit < 0).stream().map(e -> e.getKey().toString() + " -> " + e.getValue()).collect(Collectors.joining(", "));
    }

}
