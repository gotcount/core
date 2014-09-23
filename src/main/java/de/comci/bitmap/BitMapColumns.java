package de.comci.bitmap;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class BitMapColumns {

    public static BasicSchemaBuilder create() {
        return new BasicSchemaBuilder();
    }

    public static DbSchemaBuilder create(Connection conn, String table) {
        return new DbSchemaBuilder(conn, table);
    }

    private final Map<String, BitMapDimension> dimensions;
    private final int columns;
    private final List<Object[]> raw = new ArrayList<>(10000);
    private boolean isReady;
    private int size = 0;

    BitMapColumns(Map<String, BitMapDimension> dimensions) {
        this.dimensions = dimensions;
        this.columns = dimensions.size();
    }
    
    public BitMapColumns add(List<Object[]> data) {
        data.forEach(d -> add(d));
        return this;
    }

    /**
     * Add a data tuple
     *
     * @param tuple to be added, length must be equal to the number of
     * dimensions
     * @throws IllegalArgumentException if data.length does not equal the number
     * of dimensions
     * @throws IllegalArgumentException if the type of any object in data does
     * not match the dimensions type
     */
    public BitMapColumns add(final Object[] data) {

        isReady = false;

        // check null
        if (data == null) {
            throw new IllegalArgumentException("null parameter not allowed");
        }

        // check length
        if (data.length != columns) {
            throw new IllegalArgumentException(String.format("column count does not match: expected %d, received %d", dimensions.size(), data.length));
        }

        // check types
        for (BitMapDimension d : dimensions.values()) {
            if (data[d.index] != null
                    && !d.getType().isAssignableFrom(data[d.index].getClass())) {
                throw new IllegalArgumentException();
            }
        }

        this.raw.add(data);

        return this;
    }

    /**
     * Synonym for {@link BitMapColumns#build()
     *
     * @return
     */
    BitMapColumns update() {
        return build();
    }

    /**
     * Generate the bit map data structures based upon the added data
     */
    BitMapColumns build() {
        long start = System.currentTimeMillis();
        int rows = raw.size();

        dimensions.values().parallelStream().forEach(d -> {
            for (int i = 0; i < rows; i++) {
                d.set(i, raw.get(i)[d.index]);
            }
        });

        this.size += rows;

        // throw away input 
        raw.clear();
        // set state
        isReady = true;
        long buildOp = System.currentTimeMillis() - start;
        System.out.println(String.format(Locale.ENGLISH, "built maps for %,d rows in %,d ms", rows, buildOp));
        return this;
    }

    /**
     * Get count for single dimensions value
     *
     * @param <T> type of the dimensions value, must be constistent with
     * {@link Dimension#clasz}
     * @param dimension name of the dimension
     * @param value value whose count is requested
     * @return the count of the value within the selected dimension or 0 if this
     * value does not exists within the dimension
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
     * @param <T> type of the dimensions values, must be constistent with
     * {@link Dimension#clasz}
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
     * @param <T> type of the dimensions values, must be constistent with
     * {@link Dimension#clasz}
     * @param dimension name of the dimension
     * @param filters the Map of filters to be applied to the other dimensions
     * @return a Map<T, Integer> with the count for each value within dimension
     * where the given filters apply
     * @throws NoSuchElementException if no dimension with the given name exists
     * @throws IllegalArgumentException if the selected dimension is part of the
     * filters
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

    public int size() {
        return size;
    }

    private void checkReadyState() throws IllegalStateException {
        if (!isReady) {
            throw new IllegalStateException("must build before querying");
        }
    }

    public Collection<Dimension> getDimensions() {
        return Collections.<Dimension>unmodifiableCollection(dimensions.values());
    }

}
