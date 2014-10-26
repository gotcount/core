package de.comci.bitmap;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Ints;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class BitMapCollection {

    private final static Logger LOG = LoggerFactory.getLogger(BitMapCollection.class);

    public static BasicSchemaBuilder create() {
        return new BasicSchemaBuilder();
    }

    public static DbSchemaBuilder create(Connection conn, String table, String... columns) {
        return new DbSchemaBuilder(conn, table, columns);
    }

    private final Map<String, BitMapDimension> dimensions;
    private final int columns;
    private final List<Object[]> raw = new ArrayList<>(10000);
    private boolean isReady;
    private int size = 0;

    BitMapCollection(Map<String, BitMapDimension> dimensions) {
        this.dimensions = dimensions;
        this.columns = dimensions.size();
    }

    public BitMapCollection add(List<Object[]> data) {
        data.forEach(d -> add(d));
        return this;
    }
    
    /**
     * Add a data tuple
     *
     * @param data
     * @return
     * @throws IllegalArgumentException if data.length does not equal the number
     * of dimensions
     * @throws IllegalArgumentException if the type of any object in data does
     * not match the dimensions type
     */
    public BitMapCollection add(final Object... data) {

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
    BitMapCollection update() {
        return build();
    }

    /**
     * Generate the bit map data structures based upon the added data
     */
    BitMapCollection build() {
        long start = System.currentTimeMillis();
        int rows = raw.size();

        dimensions.values().parallelStream().forEach(d -> {
            for (int i = 0; i < rows; i++) {
                d.set(i, raw.get(i)[d.index]);
            }
        });
        
        // sort dimension lists
        dimensions.values().parallelStream().forEach(d -> {
            // lambda not supported here ?!?
            d.sortedMaps.sort(new Comparator<Map.Entry<Value, EWAHCompressedBitmap>>() {
                @Override
                public int compare(Map.Entry<Value, EWAHCompressedBitmap> o1, Map.Entry<Value, EWAHCompressedBitmap> o2) {
                    return o2.getValue().cardinality() - o1.getValue().cardinality();
                }                
            });
        });

        this.size += rows;

        // throw away input 
        raw.clear();
        // set state
        isReady = true;
        long buildOp = System.currentTimeMillis() - start;

        LOG.info(String.format(Locale.ENGLISH, "built map for %,d rows in %,d ms", rows, buildOp));

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
    public Multiset<Value> histogram(String dimension) {
        return histogram(dimension, 0);
    }

    public Multiset<Value> histogram(String dimension, int limit) {

        checkReadyState(dimension);
        
        long start = System.currentTimeMillis();
        final Multiset<Value> histogram = dimensions.get(dimension).histogram(limit);
        long histogramOp = System.currentTimeMillis() - start;

        LOG.info(String.format(Locale.ENGLISH, "histogram created in %,d ms without filter", histogramOp));

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
    public Multiset<Value> histogram(String dimension, Map<String, Predicate> filters) {
        return histogram(dimension, filters, 0);
    }
    
    public Multiset<Value> histogram(String dimension, Map<String, Predicate> filters, int topN) {
        
        if (filters == null || filters.isEmpty()) {
            return histogram(dimension, topN);            
        }
        
        checkReadyState(dimension);
        
        if (filters.containsKey(dimension)) {
            throw new IllegalArgumentException("cannot filter on histogram dimension");
        }

        long start = System.currentTimeMillis();
        EWAHCompressedBitmap filter = EWAHCompressedBitmap.and(
                dimensions.entrySet().parallelStream()
                    .filter(e -> filters.containsKey(e.getKey()))
                    .map(e -> e.getValue().filter(filters.get(e.getKey())))
                    .toArray(s -> new EWAHCompressedBitmap[s])
            );
        long filterOp = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        final Multiset histogram = dimensions.get(dimension).histogram(filter, topN);
        long histogramOp = System.currentTimeMillis() - start;

        LOG.info(String.format(Locale.ENGLISH, "histogram created in %,d ms with %,d ms for filtering", histogramOp, filterOp));

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

    private void checkReadyState(String dimension) throws IllegalStateException {
        checkReadyState();
        if (!dimensions.containsKey(dimension)) {
            throw new NoSuchElementException();
        }
    }

    public Collection<Dimension> getDimensions() {
        return Collections.<Dimension>unmodifiableCollection(dimensions.values());
    }

    public Dimension getDimension(String name) {
        if (!dimensions.containsKey(name)) {
            throw new NoSuchElementException(String.format("no dimension with name '%s' exists", name));
        }
        return dimensions.get(name);
    }

    /**
     * http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
     *
     * @param map
     * @return
     */
    private static <T> Map<T, Integer> sortByValue(Map<T, Integer> map, SortDirection dir) {

        List<Map.Entry<T, Integer>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, (Map.Entry<T, Integer> o1, Map.Entry<T, Integer> o2) -> dir.order().compare(o1.getValue(), o2.getValue()));

        final Map<T, Integer> result = new LinkedHashMap<>();
        list.stream().forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;

    }

    /**
     * http://stackoverflow.com/questions/4345633/simplest-way-to-iterate-through-a-multiset-in-the-order-of-element-frequency
     */
    private enum EntryComp implements Comparator<Multiset.Entry<?>> {

        DESCENDING {
                    @Override
                    public int compare(final Multiset.Entry<?> a, final Multiset.Entry<?> b) {
                        return Ints.compare(b.getCount(), a.getCount());
                    }
                },
        ASCENDING {
                    @Override
                    public int compare(final Multiset.Entry<?> a, final Multiset.Entry<?> b) {
                        return Ints.compare(a.getCount(), b.getCount());
                    }
                },
    }

    public static <E> List<Multiset.Entry<E>> getEntriesSortedByFrequency(
            final Multiset<E> ms, final boolean ascending) {
        final List<Multiset.Entry<E>> entryList = Lists.newArrayList(ms.entrySet());
        Collections.sort(entryList, ascending
                ? EntryComp.ASCENDING
                : EntryComp.DESCENDING);
        return entryList;
    }

}
