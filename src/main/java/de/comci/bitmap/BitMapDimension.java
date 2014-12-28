package de.comci.bitmap;

import com.google.common.collect.Multimap;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import de.comci.histogram.HashHistogram;
import de.comci.histogram.Histogram;
import de.comci.histogram.LimitedHashHistogram;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 * @param <T>
 */
public class BitMapDimension<T> implements Dimension<T> {

    private final String name;
    private final Class<T> clasz;
    final Map<Value<T>, EWAHCompressedBitmap> bitmap = new HashMap<>();
    final List<Map.Entry<Value<T>, EWAHCompressedBitmap>> sortedMaps = new ArrayList<>();
    final int index;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getType() {
        return clasz;
    }
    
    @Override
    public String toString() {
        return String.format("Dimension[%s/%s@%d]", name, clasz.getName(), index);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.name);
        hash = 83 * hash + Objects.hashCode(this.clasz);
        hash = 83 * hash + this.index;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!Dimension.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final Dimension<?> other = (Dimension<?>) obj;
        if (!Objects.equals(this.name, other.getName())) {
            return false;
        }
        if (!Objects.equals(this.clasz, other.getType())) {
            return false;
        }
        return true;
    }

    BitMapDimension(String name, int index, Class<T> clasz) {
        this.name = name;
        this.index = index;
        this.clasz = clasz;
    }

    BitMapDimension<T> set(int row, T value) {
        final Value valueObject = new Value(value, clasz);
        bitmap.computeIfAbsent(valueObject, k -> {
            final EWAHCompressedBitmap map = new EWAHCompressedBitmap();
            sortedMaps.add(new AbstractMap.SimpleImmutableEntry<>(valueObject, map));
            return map;
        }).set(row);
        return this;
    }
    
    public T get(int row) {
        EWAHCompressedBitmap rb = new EWAHCompressedBitmap();
        rb.set(row);
        return bitmap.entrySet().parallelStream().filter(e -> e.getValue().andCardinality(rb) > 0).findAny().get().getKey().getValue();
    }
    
    int count(T value) {
        EWAHCompressedBitmap bs = bitmap.get(new Value(value, clasz));
        return (bs != null) ? bs.cardinality() : 0;
    }

    Histogram<Value<T>> histogram() {
        return histogramWithoutFilter(null, 0);
    }

    Histogram<Value<T>> histogram(int topN) {
        return histogramWithoutFilter(null, topN);
    }
    
    Histogram<Value<T>> histogram(Map<Value<T>, Predicate> buckets, int topN) {
        return histogramWithoutFilter(buckets, topN);
    }

    Histogram<Value<T>> histogram(EWAHCompressedBitmap filter) {
        return histogram(filter, 0);
    }

    Histogram<Value<T>> histogram(EWAHCompressedBitmap filter, int topN) {
        return histogramWithFilter(filter, null, topN);
    }
    
    Histogram<Value<T>> histogram(EWAHCompressedBitmap filter, Map<Value<T>, Predicate> buckets, int topN) {
        return histogramWithFilter(filter, buckets, topN);
    }

    EWAHCompressedBitmap filter(final Predicate<T> p) {
        final EWAHCompressedBitmap[] maps = bitmap.entrySet()
                .parallelStream()
                .filter(e -> p.test(e.getKey().getValue()))
                .map(e -> e.getValue())
                .toArray(s -> new EWAHCompressedBitmap[s]);

        switch (maps.length) {
            case 0:
                return new EWAHCompressedBitmap();
            case 1:
                return maps[0];
        }

        return EWAHCompressedBitmap.or(maps);
    }

    private Histogram<Value<T>> histogramWithoutFilter(Map<Value<T>, Predicate> buckets, int limit) {

        Histogram<Value<T>> histogram = new LimitedHashHistogram<>(limit);

        Collection<Map.Entry<Value<T>, EWAHCompressedBitmap>> sublist;
        
        if (buckets == null || buckets.isEmpty()) {
            if (limit > 0) {
                sublist = sortedMaps.subList(0, Math.min(limit, sortedMaps.size()));
            } else if (limit < 0) {
                int from = Math.max(sortedMaps.size() + limit, 0);
                int to = sortedMaps.size();
                sublist = sortedMaps.subList(from, to);
            } else {
                sublist = sortedMaps;
            }
        } else {
            sublist = getBuckets(buckets);
        }

        sublist.forEach((e) -> {
            histogram.set(e.getKey(), e.getValue().cardinality());
        });

        return histogram;
    }

    private Histogram<Value<T>> histogramWithFilter(EWAHCompressedBitmap filter, Map<Value<T>, Predicate> buckets, int limit) {

        Histogram<Value<T>> histogram = new HashHistogram<>();

        Comparator<Map.Entry<Value<T>,Integer>> c;
        if (limit >= 0) {
            c = (a,b) -> b.getValue() - a.getValue();
        } else {
            c = (a,b) -> a.getValue() - b.getValue();
        }
        
        // get original bitmap OR bucket bitmaps
        Collection<Map.Entry<Value<T>, EWAHCompressedBitmap>> bitmaps;
        if (buckets == null || buckets.isEmpty()) {
            bitmaps = bitmap.entrySet();
        } else {
            bitmaps = getBuckets(buckets);
        }
        
        Stream<AbstractMap.SimpleEntry<Value<T>, Integer>> stream = bitmaps.parallelStream()
                .map(e -> new HashMap.SimpleEntry<>(e.getKey(), filter.andCardinality(e.getValue())))
                .filter(e -> e.getValue() > 0)
                .sorted(c);
            
        if (limit != 0) {            
            stream = stream.limit(Math.abs(limit));
        }
        
        stream.forEach(e -> {
            histogram.set(e.getKey(), e.getValue());
        });

        return histogram;
    }

    private Collection<Map.Entry<Value<T>, EWAHCompressedBitmap>> getBuckets(Map<Value<T>, Predicate> buckets) {
        Collection<Map.Entry<Value<T>, EWAHCompressedBitmap>> bitmaps;
        ConcurrentMap<Value<T>, EWAHCompressedBitmap> calculatedBuckets = new ConcurrentHashMap<>();
        this.bitmap.entrySet().stream().forEach((e) -> {
            final T currentValue = e.getKey().getValue();
            boolean found = false;
            for (Map.Entry<Value<T>,Predicate> b : buckets.entrySet()) {
                if (b.getValue().test(currentValue)) {
                    // value assigned to at least one bucket
                    // a value will be assigned to only one bucket
                    calculatedBuckets.compute(b.getKey(), (k,v) -> {
                        return (v == null) ? e.getValue() : e.getValue().or(v);
                    });
                    found = true;
                    break;
                }
            }            
            if (!found) {
                // values not assigned to any bucket
                calculatedBuckets.compute(Value.bucket(e.getKey().getType(), ""), (k,v) -> (v == null) ? e.getValue() : e.getValue().or(v));
            }            
        });
        bitmaps = calculatedBuckets.entrySet();
        return bitmaps;
    }

    @Override
    public long getCardinality() {
        return bitmap.size();
    }

    void build() {
        // sort values by cardinality
        sortedMaps.sort((a, b) -> b.getValue().cardinality() - a.getValue().cardinality());
    }   

}
