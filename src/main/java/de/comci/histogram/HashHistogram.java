/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public Integer remove(T t) {
        return map.remove(t);
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
        if (!HashHistogram.class.isAssignableFrom(obj.getClass())) {
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
        return map.entrySet().stream().map(e -> e.toString()).collect(Collectors.joining(", "));
    }

    @Override
    public Iterator<Entry<T, Integer>> iterator() {
        return stream().iterator();
    }

    @Override
    public Set<T> keySet() {
        return map.keySet();
    }

    @Override
    public Stream<Entry<T, Integer>> stream() {
        return map.entrySet().stream().map(e -> (Entry<T, Integer>)new SimpleEntry<>(e.getKey(), e.getValue()));
    }
    
    public static final class SimpleEntry<T> implements Entry<T, Integer> {

        private final Integer count;
        private final T key;

        public SimpleEntry(T key, Integer count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public T getKey() {
            return key;
        }

        @Override
        public Integer getCount() {
            return count;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 11 * hash + Objects.hashCode(this.count);
            hash = 11 * hash + Objects.hashCode(this.key);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!Histogram.Entry.class.isAssignableFrom(obj.getClass())) {
                return false;
            }
            final SimpleEntry<?> other = (SimpleEntry<?>) obj;
            if (!Objects.equals(this.count, other.count)) {
                return false;
            }
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return String.format("%s -> %d", key, count);
        }

    }
    
}
