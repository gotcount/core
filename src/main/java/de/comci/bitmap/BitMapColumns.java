package de.comci.bitmap;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final List<Object[]> raw = new ArrayList<>(1000);
    private boolean isReady;

    /**
     * Add a new dimension
     * 
     * @param name of the dimension, must be unique
     * @param clasz of the dimensions values
     * @return 
     */
    public BitMapColumns dimension(String name, Class clasz) {
        dimensions.put(name, new Dimension(name, dimensions.size(), clasz));
        return this;
    }

    /**
     * Add a data row or tuple
     * 
     * @param data to be added, length must be equal to the number of dimensions
     * @throws IllegalStateException if bitmaps have already been built
     * @throws IllegalArgumentException if data.length does not equal the number of dimensions
     * @throws IllegalArgumentException if the type of any object in data does not match the dimensions type
     */
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

    /**
     * Generate the bit map data structures based upon the added data
     */
    public void build() {
        long start = System.currentTimeMillis();
        int rows = raw.size();
        
        dimensions.values().parallelStream().forEach(d -> {
            for (int i = 0; i < rows; i++) {
                d.set(i, raw.get(i)[d.index]);
            }
        });        
        
        // throw away input 
        raw.clear();
        // set state
        isReady = true;
        long buildOp = System.currentTimeMillis() - start;
        System.out.println(String.format(Locale.ENGLISH, "built maps for %,d rows in %,d ms", rows, buildOp));
    }

    /**
     * Get count for single dimensions value
     * 
     * @param <T> type of the dimensions value, must be constistent with {@link Dimension#clasz}
     * @param dimension name of the dimension 
     * @param value value whose count is requested
     * @return the count of the value within the selected dimension or 0 if this value does not exists within the dimension
     * @throws NoSuchElementException if no dimension with the given name exists
     */
    public <T> int count(String dimension, T value) {
        checkReadyState();
        if (!dimensions.containsKey(dimension)) {
            throw new NoSuchElementException();
        }
        return dimensions.get(dimension).count(value);
    }

    /**
     * Get histogram for single dimension
     * 
     * @param <T> type of the dimensions values, must be constistent with {@link Dimension#clasz}
     * @param dimension name of the dimension
     * @return a Map<T, Integer> with the count for each value within dimension
     * @throws NoSuchElementException if no dimension with the given name exists
     */
    public <T> Map<T, Integer> histogram(String dimension) {
        checkReadyState();
        if (!dimensions.containsKey(dimension)) {
            throw new NoSuchElementException();
        }
        
        long start = System.currentTimeMillis();
        final Map histogram = dimensions.get(dimension).histogram();
        long histogramOp = System.currentTimeMillis() - start;
        
        System.out.println(String.format(Locale.ENGLISH, "histogram created in %,d ms without filter", histogramOp));
        
        return histogram;
    }

    /**
     * Get histogram for single dimension
     * 
     * @param <T> type of the dimensions values, must be constistent with {@link Dimension#clasz}
     * @param dimension name of the dimension
     * @param filters the Map of filters to be applied to the other dimensions
     * @return a Map<T, Integer> with the count for each value within dimension where the given filters apply
     * @throws NoSuchElementException if no dimension with the given name exists
     * @throws IllegalArgumentException if the selected dimension is part of the filters
     */
    public <T> Map<T, Integer> histogram(String dimension, Map<String, Predicate> filters) {
        checkReadyState();
        if (!dimensions.containsKey(dimension)) {
            throw new NoSuchElementException();
        }
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
