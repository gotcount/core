/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class LimitedHashHistogram<T> extends HashHistogram<T> {

    private final int limit;
    private final PriorityQueue<Histogram.Entry<T, Integer>> sortedElements;

    public LimitedHashHistogram() {
        this(0);
    }

    public LimitedHashHistogram(int limit) {
        super();

        this.limit = limit;

        Comparator<Entry<T, Integer>> c;
        if (limit < 0) {
            c = (a, b) -> b.getCount() - a.getCount();
        } else {
            c = (a, b) -> a.getCount() - b.getCount();
        }

        sortedElements = new PriorityQueue<>(c);
    }

    @Override
    public void set(T t, int count) {
        
        if (limit != 0 && size() >= Math.abs(limit)) {
            final Entry<T, Integer> last = sortedElements.peek();
            if ((limit > 0 && count > last.getCount())
                    || (limit < 0 && count < last.getCount())) {
                super.remove(last.getKey());
                sortedElements.poll();
                super.set(t, count);
                sortedElements.add(new SimpleEntry<>(t, count));
            }
        } else {
            super.set(t, count);
            sortedElements.add(new SimpleEntry<>(t, count));
        }

    }

    @Override
    public Integer remove(T t) {
        Integer c = super.remove(t);
        sortedElements.remove(new SimpleEntry<>(t, c));
        return c;
    }

    @Override
    public String toString() {
        return sortedElements.stream().map(e -> e.getKey().toString() + " -> " + e.getCount()).collect(Collectors.joining(", "));
    }
    
}
