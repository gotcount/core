/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 * @param <T>
 */
public interface Histogram<T> {

    /**
     * Returns the count for the given key
     *
     * @param key
     * @return a non-negative integer representing the count of the key within
     * the histogram
     * @throws NoSuchElementException if the provided key does not exist
     */
    Integer get(T key);

    TreeSet<T> keySet(boolean ascending);
    
    TreeSet<Map.Entry<T, Integer>> entrySet(boolean ascending);

    void set(T key, int counkey);

    int size();

    void remove(T key);

    boolean has(T key);
    
    void clear();

}
