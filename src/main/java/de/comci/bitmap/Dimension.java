package de.comci.bitmap;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
class Dimension<T> {
    
    private final String name;
    final Class<T> clasz;
    final int index;
    final Map<T, EWAHCompressedBitmap> bitmap = new HashMap<>();

    Dimension(String name, int index, Class<T> clasz) {
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

    public Map<T, Integer> histogram() {
        return histogram(b -> b.cardinality());
    }

    public Map<T, Integer> histogram(EWAHCompressedBitmap filter) {
        return histogram(b -> b.andCardinality(filter));
    }

    public String getName() {
        return name;
    }
    
    private Map<T, Integer> histogram(Function<EWAHCompressedBitmap, Integer> mapping) {
        Map<T, Integer> h = new HashMap<>(bitmap.size());
        bitmap.entrySet().stream().forEach((e) -> {
            h.put(e.getKey(), mapping.apply(e.getValue()));
        });
        return h;
    }
    
    EWAHCompressedBitmap filter(final Predicate<T> p) {
        final EWAHCompressedBitmap[] maps = bitmap.entrySet()
                .parallelStream()
                .filter(e -> p.test(e.getKey()))
                .map(e -> e.getValue())
                .toArray(s -> new EWAHCompressedBitmap[s]);
        
        switch (maps.length) {
            case 0: return new EWAHCompressedBitmap();
            case 1: return maps[0];
        }
        
        return EWAHCompressedBitmap.or(maps);
    }
        
}
