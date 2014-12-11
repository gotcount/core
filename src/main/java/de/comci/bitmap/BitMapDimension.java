package de.comci.bitmap;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import de.comci.histogram.HashHistogram;
import de.comci.histogram.Histogram;
import de.comci.histogram.LimitedHashHistogram;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
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

    int count(T value) {
        EWAHCompressedBitmap bs = bitmap.get(new Value(value, clasz));
        return (bs != null) ? bs.cardinality() : 0;
    }

    Histogram<Value<T>> histogram() {
        return histogramWithoutFilter(0);
    }

    Histogram<Value<T>> histogram(int topN) {
        return histogramWithoutFilter(topN);
    }

    Histogram<Value<T>> histogram(EWAHCompressedBitmap filter) {
        return histogram(filter, 0);
    }

    Histogram<Value<T>> histogram(EWAHCompressedBitmap filter, int topN) {
        return histogramWithFilter(filter, topN);
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

    private Histogram<Value<T>> histogramWithoutFilter(int limit) {

        Histogram<Value<T>> histogram = new LimitedHashHistogram<>(limit);

        List<Map.Entry<Value<T>, EWAHCompressedBitmap>> sublist;
        if (limit > 0) {
            sublist = sortedMaps.subList(0, Math.min(limit, sortedMaps.size()));
        } else if (limit < 0) {
            int from = Math.max(sortedMaps.size() + limit, 0);
            int to = sortedMaps.size();
            sublist = sortedMaps.subList(from, to);
        } else {
            sublist = sortedMaps;
        }

        sublist.forEach((e) -> {
            histogram.set(e.getKey(), e.getValue().cardinality());
        });

        return histogram;
    }

    private Histogram<Value<T>> histogramWithFilter(EWAHCompressedBitmap filter, int limit) {

        Histogram<Value<T>> histogram = new HashHistogram<>();

        Comparator<Map.Entry<Value<T>,Integer>> c;
        if (limit >= 0) {
            c = (a,b) -> b.getValue() - a.getValue();
        } else {
            c = (a,b) -> a.getValue() - b.getValue();
        }
        
        Stream<AbstractMap.SimpleEntry<Value<T>, Integer>> stream = sortedMaps.parallelStream()
                .map(e -> new HashMap.SimpleEntry<>(e.getKey(), filter.andCardinality(e.getValue())))
                .filter(e -> e.getValue() > 0)
                .sorted(c);
            
        if (limit != 0) {            
            stream = stream.limit(Math.abs(limit));
        }
        
        stream.forEach(e -> {
            histogram.set(e.getKey(), e.getValue());
        });
        
        /*
        int currentLimit = 0;
        for (Map.Entry<Value<T>, EWAHCompressedBitmap> e : sortedMaps) {

            if (limit > 0
                    && (histogram.size() == Math.abs(limit))
                    && (e.getValue().cardinality() <= currentLimit)) {
                // we do not need to check the remaining values
                // they cannot contribute anything of interest
                break;
            }

            int currentValueCount = filter.andCardinality(e.getValue());
            if (currentValueCount > 0) {
                histogram.set(e.getKey(), currentValueCount);
                if (limit > 0) {
                    currentLimit = Math.max(currentLimit, currentValueCount);
                }
            }

        }
        */

        return histogram;
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
