/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import de.comci.histogram.Histogram;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class HistogramBuilder<T> {

    private final static Logger LOG = LoggerFactory.getLogger(HistogramBuilder.class);

    // mandatory
    private final String dimension;
    private final BitMapCollection collection;

    // optional with default values
    private Map<String, Predicate> filters = new HashMap<>();
    private Map<Value<T>, Predicate> buckets = new HashMap<>();
    private int limit = 0;

    HistogramBuilder(String dimension, BitMapCollection collection) {
        this.dimension = dimension;
        this.collection = collection;
    }

    public HistogramBuilder setFilters(Map<String, Predicate> filters) {
        this.filters = filters;
        return this;
    }
    
    public HistogramBuilder filter(String name, Predicate logic) {
        if (this.filters == null) {
            this.filters = new HashMap<>();
        }
        this.filters.put(name, logic);
        return this;
    }

    public HistogramBuilder setBuckets(Map<Value<T>, Predicate> buckets) {
        this.buckets = buckets;
        return this;
    }
    
    public HistogramBuilder bucket(Value<T> name, Predicate logic) {
        if (this.buckets == null) {
            this.buckets = new HashMap<>();
        }
        this.buckets.put(name, logic);
        return this;
    }

    public HistogramBuilder setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public Histogram<Value> build() {

        boolean hasFilter = (filters != null && !filters.isEmpty());
        boolean hasBuckets = (buckets != null && !buckets.isEmpty());

        long start = System.currentTimeMillis();
        Histogram<Value> histogram = null;
        
        if (hasFilter) {
            
            // filter are independant of buckets
            EWAHCompressedBitmap filter = collection.getFilter(filters);
            long filterOp = System.currentTimeMillis() - start;
            start = System.currentTimeMillis();
            
            if (hasBuckets) {
                // filter && buckets
                histogram = ((BitMapDimension)collection.getDimension(dimension)).histogram(filter, buckets, limit);
                long histogramOp = System.currentTimeMillis() - start;

                LOG.info(String.format(Locale.ENGLISH, "histogram using buckets created in %,d ms with %,d ms for filtering", histogramOp, filterOp));
            } else {
                // filter only                
                histogram = ((BitMapDimension)collection.getDimension(dimension)).histogram(filter, limit);
                long histogramOp = System.currentTimeMillis() - start;

                LOG.info(String.format(Locale.ENGLISH, "histogram created in %,d ms with %,d ms for filtering", histogramOp, filterOp));
            }
        } else {
            if (hasBuckets) {
                // buckets only
                histogram = ((BitMapDimension)collection.getDimension(dimension)).histogram(buckets, limit);
                long histogramOp = System.currentTimeMillis() - start;
                LOG.info(String.format(Locale.ENGLISH, "histogram created in %,d ms without filter", histogramOp));
            } else {
                // neither filter nor buckets
                histogram = ((BitMapDimension)collection.getDimension(dimension)).histogram(limit);
                long histogramOp = System.currentTimeMillis() - start;
                LOG.info(String.format(Locale.ENGLISH, "histogram created in %,d ms without filter", histogramOp));
            }
        }
        
        return histogram;
        
    }

}
