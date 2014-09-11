/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.cubetree;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class DimensionStatistics {

    private final Dimension dimension;
    private final Multiset<Object> values = HashMultiset.create();
    private LinkedHashMap<Object,Integer> histogram;

    public DimensionStatistics(Dimension d) {
        this.dimension = d;
    }

    public void add(Object value) {
        values.add(value);
    }

    public Map<Object, Integer> getHistogram() {
        if (histogram == null) {
            // originally I used guava here. But the guava way does not
            // permit null values as keys in the histogram
            histogram = new LinkedHashMap<>();
            values.entrySet().stream().sorted((Multiset.Entry<Object> o1, Multiset.Entry<Object> o2) -> {
                return o2.getCount() - o1.getCount();
            }).forEach(e -> histogram.put(e.getElement(), e.getCount()));
        }        
        return histogram;
    }

}
