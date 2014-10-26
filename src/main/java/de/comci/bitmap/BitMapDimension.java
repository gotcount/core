package de.comci.bitmap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 * @param <T>
 */
public class BitMapDimension<T> implements Dimension {

    private final String name;
    private final Class<T> clasz;
    private final Map<Value<T>, EWAHCompressedBitmap> bitmap = new HashMap<>();
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BitMapDimension<?> other = (BitMapDimension<?>) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.clasz, other.clasz)) {
            return false;
        }
        return true;
    }

    BitMapDimension(String name, int index, Class<T> clasz) {
        this.name = name;
        this.index = index;
        this.clasz = clasz;
    }

    void set(int row, T value) {
        final Value valueObject = new Value(value, clasz);
        bitmap.computeIfAbsent(valueObject, k -> {
            final EWAHCompressedBitmap map = new EWAHCompressedBitmap();
            sortedMaps.add(new AbstractMap.SimpleImmutableEntry<>(valueObject, map));
            return map;
        }).set(row);
    }

    int count(T value) {
        EWAHCompressedBitmap bs = bitmap.get(new Value(value, clasz));
        return (bs != null) ? bs.cardinality() : 0;
    }

    Multiset<Value<T>> histogram() {
        return histogramWithoutFilter(0);
    }

    Multiset<Value<T>> histogram(int topN) {
        return histogramWithoutFilter(topN);
    }

    Multiset<Value<T>> histogram(EWAHCompressedBitmap filter) {
        return histogram(b -> b.andCardinality(filter), 0);
    }

    Multiset<Value<T>> histogram(EWAHCompressedBitmap filter, int topN) {
        return histogram(b -> b.andCardinality(filter), topN);
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

    private Multiset<Value<T>> histogramWithoutFilter(int limit) {
        Multiset<Value<T>> h = HashMultiset.create(sortedMaps.size());

        List<Map.Entry<Value<T>, EWAHCompressedBitmap>> sublist;
        if (limit > 0) {
            sublist = sortedMaps.subList(0, limit);
        } else if (limit < 0) {
            int from = Math.max(sortedMaps.size() + limit, 0);
            int to = sortedMaps.size();
            sublist = sortedMaps.subList(from, to);
        } else {
            sublist = sortedMaps;
        }

        sublist.forEach((e) -> {
            int count = e.getValue().cardinality();
            boolean canAdd = true;
            // remove value(s) to small (or large) to be part of the top (or bottom) n elements
            if (limit != 0 && h.elementSet().size() == Math.abs(limit) && !h.elementSet().contains(e.getKey())) {
                canAdd = false;
                for (Iterator<Entry<Value<T>>> i = h.entrySet().iterator(); i.hasNext();) {
                    if ((limit > 0 && i.next().getCount() < count)
                            || (limit < 0 && i.next().getCount() > count)) {
                        i.remove();
                        canAdd = true;
                        break;
                    }
                }
            }
            if (canAdd) {
                h.add(e.getKey(), count);
            }
        });
        return h;
    }

    private Multiset<Value<T>> histogram(Function<EWAHCompressedBitmap, Integer> mapping, int limit) {
        Multiset<Value<T>> h = HashMultiset.create(sortedMaps.size());

        int border = (limit > 0) ? 0 : Integer.MAX_VALUE;

        for (Map.Entry<Value<T>, EWAHCompressedBitmap> e : sortedMaps) {
            if (h.elementSet().size() == Math.abs(limit)
                    && ((limit > 0 && e.getValue().cardinality() <= border)
                    || (limit < 0 && e.getValue().cardinality() >= border))) {
                // we do not need to check the remaining values
                // they cannot contribute anything of interest
                break;
            }

            int count = mapping.apply(e.getValue());
            boolean canAdd = true;
            // remove value(s) to small (or large) to be part of the top (or bottom) n elements
            if (limit != 0 && h.elementSet().size() == Math.abs(limit) && !h.elementSet().contains(e.getKey())) {
                canAdd = false;
                for (Iterator<Entry<Value<T>>> i = h.entrySet().iterator(); i.hasNext();) {
                    if ((limit > 0 && i.next().getCount() < count)
                            || (limit < 0 && i.next().getCount() > count)) {
                        i.remove();
                        canAdd = true;
                        break;
                    }
                }
            }
            if (canAdd) {
                h.add(e.getKey(), count);
                if (limit > 0) {
                    border = Math.max(border, count);
                } else if (limit < 0) {
                    border = Math.min(border, count);
                }
            }
        }

        return h;
    }

}
