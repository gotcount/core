/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.google.common.base.Functions;
import com.google.common.collect.Ordering;
import java.util.Comparator;
import java.util.Map;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public enum SortDirection {

    ASCENDING(Ordering.natural().nullsFirst()),
    DESCENDING(Ordering.natural().nullsFirst().reverse());

    private Ordering order;

    private SortDirection(Ordering order) {
        this.order = order;
    }

    <K, V> Comparator order(Map<K, ? extends V> map) {
        return order.onResultOf(Functions.forMap(map, null));
    }
    
    Comparator<Integer> order() {
        return (this == ASCENDING) ? up : down;
    }

    private final static Comparator<Integer> up = (Integer o1, Integer o2) -> o1.compareTo(o2);

    private final static Comparator<Integer> down = (Integer o1, Integer o2) -> o2.compareTo(o1);

}
