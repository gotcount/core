/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.google.common.collect.Multiset;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class BitMapColumns {

    private final Map<String, Dimension> dimensions = new HashMap<>();
    private final List<Object[]> raw = new ArrayList<Object[]>(1000);
    private boolean isReady;

    public BitMapColumns dimension(String name, Class clasz) {
        dimensions.put(name, new Dimension(name, dimensions.size(), clasz));
        return this;
    }

    public void add(final Object[] data) {
        if (isReady) {
            throw new IllegalStateException("already built");
        }
        if (data.length != dimensions.size()) {
            throw new IllegalArgumentException();
        }
        for (Dimension d : dimensions.values()) {
            if (data[d.index] != null
                    && !d.clasz.isAssignableFrom(data[d.index].getClass())) {
                throw new IllegalArgumentException();
            }
        }
        raw.add(data);
    }

    public void build() {
        long start = System.currentTimeMillis();
        int rows = raw.size();
        dimensions.values().forEach(d -> d.setSize(rows));
        
        dimensions.values().parallelStream().forEach(d -> {
            d.setSize(rows);
            for (int i = 0; i < rows; i++) {
                Object[] row = raw.get(i);                
                d.set(i, row[d.index]);
            }
        });        
        
        // throw away input 
        raw.clear();
        // set state
        isReady = true;
        long buildOp = System.currentTimeMillis() - start;
        System.out.println(String.format(Locale.ENGLISH, "built maps for %,d rows in %,d ms", rows, buildOp));
    }

    <T> int count(String dimension, T value) {
        checkReadyState();
        if (!dimensions.containsKey(dimension)) {
            throw new NoSuchElementException();
        }
        return dimensions.get(dimension).count(value);
    }

    <T> Map<T, Integer> histogram(String dimension) {
        checkReadyState();
        long start = System.currentTimeMillis();
        final Map histogram = dimensions.get(dimension).histogram();
        long histogramOp = System.currentTimeMillis() - start;
        
        System.out.println(String.format(Locale.ENGLISH, "histogram created in %,d ms without filter", histogramOp));
        
        return histogram;
    }

    <T> Map<T, Integer> histogram(String dimension, Map<String, Predicate> filters) {
        checkReadyState();
        if (filters.containsKey(dimension)) {
            throw new IllegalArgumentException("cannot filter on histogram dimension");
        }
        
        long start = System.currentTimeMillis();
        EWAHCompressedBitmap filter = EWAHCompressedBitmap.and(
                dimensions.entrySet().stream()
                    .filter(e -> filters.containsKey(e.getKey()))
                    .map(e -> e.getValue().filter(filters.get(e.getKey())))
                    .toArray(s -> new EWAHCompressedBitmap[s])
            );        
        long filterOp = System.currentTimeMillis() - start;
        
        start = System.currentTimeMillis();
        final Map histogram = dimensions.get(dimension).histogram(filter);        
        long histogramOp = System.currentTimeMillis() - start;
        
        System.out.println(String.format(Locale.ENGLISH, "histogram created in %,d ms with %,d ms for filtering", histogramOp, filterOp));
        
        return histogram;
    }

    private void checkReadyState() throws IllegalStateException {
        if (!isReady) {
            throw new IllegalStateException("must build before querying");
        }
    }

}
