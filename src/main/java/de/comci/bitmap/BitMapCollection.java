package de.comci.bitmap;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Ints;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IntIterator;
import de.comci.histogram.Histogram;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class BitMapCollection {

    private final static Logger LOG = LoggerFactory.getLogger(BitMapCollection.class);

    public static DimensionBuilder create() {
        return new DefaultDimensionBuilder();
    }

    public static DimensionBuilder create(Connection conn, String table) {
        return new JooqDimensionBuilder(conn, table);
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
                throw new IllegalArgumentException(String.format("supplied data type '%s' does not match expected '%s' for dimension '%s'", data[d.index].getClass(), d.getType(), d.getName()));
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

        LOG.info(String.format(Locale.ENGLISH, "building map for %,d row", rows));

        dimensions.values().parallelStream().forEach(d -> {
            for (int i = 0; i < rows; i++) {
                d.set(i, raw.get(i)[d.index]);
            }
            LOG.info(String.format("dimension '%s' built", d.getName()));
        });

        // sort dimension lists
        dimensions.values().parallelStream().forEach(d -> {
            d.build();
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
     * Will return the size after applying the supplied filters. E.g. the number
     * of rows in the collection satisfying all filter conditions at once.
     *
     * @param filters
     * @return
     */
    public int size(Map<String, Predicate> filters) {

        if (filters == null || filters.isEmpty()) {
            return size();
        }

        long start = System.currentTimeMillis();
        EWAHCompressedBitmap filter = getFilter(filters);
        long filterOp = System.currentTimeMillis() - start;

        LOG.info(String.format(Locale.ENGLISH, "count with filters created in %,d ms", filterOp));

        return filter.cardinality();
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
    public Histogram<Value> histogram(String dimension) {
        return histogram(dimension, 0);
    }

    public Histogram<Value> histogram(String dimension, int limit) {

        checkReadyState(dimension);

        long start = System.currentTimeMillis();
        final Histogram<Value> histogram = dimensions.get(dimension).histogram(limit);
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
    public Histogram<Value> histogram(String dimension, Map<String, Predicate> filters) {
        return histogram(dimension, filters, 0);
    }

    public Histogram<Value> histogram(String dimension, Map<String, Predicate> filters, int topN) {

        if (filters == null || filters.isEmpty()) {
            return histogram(dimension, topN);
        }

        checkReadyState(dimension);

        long start = System.currentTimeMillis();
        EWAHCompressedBitmap filter = getFilter(filters);
        long filterOp = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        final Histogram histogram = dimensions.get(dimension).histogram(filter, topN);
        long histogramOp = System.currentTimeMillis() - start;

        LOG.info(String.format(Locale.ENGLISH, "histogram created in %,d ms with %,d ms for filtering", histogramOp, filterOp));

        return histogram;
    }
    
    public Collection<Object[]> getData(int offset, int limit, String... dimensions) {
        return getData(offset, limit, Arrays.asList(dimensions));
    }
    
    public Collection<Object[]> getData(int offset, int limit, Collection<String> dimensions) {        
        return getData(null, offset, limit, dimensions);
    }
    
    public Collection<Object[]> getData(Map<String, Predicate> filter, int offset, int limit, Collection<String> dimensions) {
        
        if (offset < 0) {
            offset = 0;
        }
        
        if (limit < 1) {
            limit = 100;
        }
        
        if (offset >= size()) {
            throw new IndexOutOfBoundsException();
        }
        
        final List<Map.Entry<String, BitMapDimension>> selectedDimensions = 
                dimensions.stream()
                        .filter(d -> this.dimensions.containsKey(d) || d.equals("ROW"))
                        .map(d -> new HashMap.SimpleEntry<>(d, d.equals("ROW") ? null : this.dimensions.get(d)))
                        .collect(Collectors.toList());
        
        final int cols = selectedDimensions.size();
        
        Collection<Object[]> outputBuffer;
        
        if (filter == null || filter.isEmpty()) {
            // create output buffer
            outputBuffer = new LinkedList<>();

            // grap data from dimensions
            for (int row = offset; row < size() && row < offset + limit ; row++) {
                final Object[] tupel = new Object[cols];
                for (int i = 0; i < cols; i++) {
                    Map.Entry<String, BitMapDimension> dim = selectedDimensions.get(i);
                    tupel[i] = (dim.getKey().equals("ROW")) ? row : dim.getValue().get(row);
                }
                outputBuffer.add(tupel);
            }
        } else {
            EWAHCompressedBitmap bitmapFilter = getFilter(filter);
            outputBuffer = new ArrayList<>(bitmapFilter.cardinality());

            Iterable<Integer> i = () -> bitmapFilter.iterator();
            Stream<Integer> filteredRows = StreamSupport.stream(i.spliterator(), false);

            filteredRows.skip(offset).limit(limit).forEach(row -> {
                final Object[] tupel = new Object[cols];
                for (int c = 0; c < cols; c++) {                
                    Map.Entry<String, BitMapDimension> dim = selectedDimensions.get(c);
                    tupel[c] = (dim.getKey().equals("ROW")) ? row : dim.getValue().get(row);
                }
                outputBuffer.add(tupel);
            });
        }
                
        return outputBuffer;
    }
    
    public Collection<Object[]> getData(Map<String, Predicate> filter, int offset, int limit, String... dimensions) {
        
        return getData(filter, offset, limit, Arrays.asList(dimensions));
    }

    private EWAHCompressedBitmap getFilter(Map<String, Predicate> filters) {
        EWAHCompressedBitmap filter = EWAHCompressedBitmap.and(
                dimensions.entrySet().parallelStream()
                .filter(e -> filters.containsKey(e.getKey()))
                .map(e -> e.getValue().filter(filters.get(e.getKey())))
                .toArray(s -> new EWAHCompressedBitmap[s])
        );
        return filter;
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
