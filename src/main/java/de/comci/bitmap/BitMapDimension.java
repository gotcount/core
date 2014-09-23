package de.comci.bitmap;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class BitMapDimension<T> implements Dimension<T> {

    private final String name;
    private final Class<T> clasz;
    private final Map<T, EWAHCompressedBitmap> bitmap = new HashMap<>();
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
        if (this.index != other.index) {
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
        bitmap.computeIfAbsent(value, k -> new EWAHCompressedBitmap()).set(row);
    }

    int count(T value) {
        EWAHCompressedBitmap bs = bitmap.get(value);
        return (bs != null) ? bs.cardinality() : 0;
    }

    Map<T, Integer> histogram() {
        return histogram(b -> b.cardinality());
    }

    Map<T, Integer> histogram(EWAHCompressedBitmap filter) {
        return histogram(b -> b.andCardinality(filter));
    }

    EWAHCompressedBitmap filter(final Predicate<T> p) {
        final EWAHCompressedBitmap[] maps = bitmap.entrySet()
                .parallelStream()
                .filter(e -> p.test(e.getKey()))
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

    private Map<T, Integer> histogram(Function<EWAHCompressedBitmap, Integer> mapping) {
        Map<T, Integer> h = new HashMap<>(bitmap.size());
        bitmap.entrySet().stream().forEach((e) -> {
            h.put(e.getKey(), mapping.apply(e.getValue()));
        });
        return h;
    }

}
