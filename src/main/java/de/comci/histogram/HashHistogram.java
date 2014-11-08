/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 * @param <T>
 */
public class HashHistogram<T> implements Histogram<T> {

    private final Map<T, Integer> map = new HashMap<>();

    @Override
    public void set(T t, int count) {
        map.compute(t, (k, v) -> {
            return count;
        });
    }

    @Override
    public void remove(T t) {
        map.remove(t);
    }

    @Override
    public boolean has(T t) {
        return map.containsKey(t);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Integer get(T key) {
        final Integer value = map.get(key);
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public TreeSet<T> keySet(boolean ascending) {

        Comparator<T> c = (a, b) -> ifZero(map.get(a).compareTo(map.get(b)), 1);
        if (!ascending) {
            c = (a, b) -> ifZero(map.get(b).compareTo(map.get(a)),1);
        }
        
        TreeSet<T> t = new TreeSet<>(c);
        t.addAll(map.keySet());
        return t;

    }
    
    private static int ifZero(int value, int inCase) {
        return (value == 0) ? inCase : value;
    }

    @Override
    public TreeSet<Map.Entry<T, Integer>> entrySet(boolean ascending) {

        Comparator<Map.Entry<T, Integer>> c = (a, b) -> ifZero(a.getValue().compareTo(b.getValue()),1);
        if (!ascending) {
            c = (a, b) -> ifZero(b.getValue().compareTo(a.getValue()),1);
        }

        TreeSet<Map.Entry<T, Integer>> t = new TreeSet<>(c);
        t.addAll(map.entrySet());
        return t;

    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.map);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }
        final HashHistogram<?> other = (HashHistogram<?>) obj;
        if (!Objects.equals(this.map, other.map)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return entrySet(true).stream().map(e -> e.getKey().toString() + " -> " + e.getValue()).collect(Collectors.joining(", "));
    }
    
}
