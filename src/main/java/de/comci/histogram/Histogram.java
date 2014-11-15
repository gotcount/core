/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 * @param <T>
 */
public interface Histogram<T> extends Iterable<Histogram.Entry<T, Integer>> {

    /**
     * Returns the count for the given key
     *
     * @param key
     * @return a non-negative integer representing the count of the key within
     * the histogram
     * @throws NoSuchElementException if the provided key does not exist
     */
    Integer get(T key);

    void set(T key, int counkey);

    int size();

    Integer remove(T key);

    boolean has(T key);
    
    void clear();
    
    Set<T> keySet();
    
    interface Entry<T, Integer> {
        
        T getKey();
        
        Integer getCount();
        
    }
    
    Stream<Entry<T, Integer>> stream();

}
