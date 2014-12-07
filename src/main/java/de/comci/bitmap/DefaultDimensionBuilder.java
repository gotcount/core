/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class DefaultDimensionBuilder implements DimensionBuilder {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultDimensionBuilder.class);

    protected final Map<String, BitMapDimension> dimensions = new HashMap<>();
    protected CollectionBuilder builder;

    @Override
    public DimensionBuilder dimension(String name, Class type) {
        return dimension(name, type, null);
    }

    @Override
    public DimensionBuilder dimension(String name, Class type, Double precision) {
        
        if (builder != null) {
            LOG.info("cannot alter dimensions when data has already been added");
            throw new IllegalStateException("cannot alter dimensions when data has already been added");
        }

        BitMapDimension d;
        if (Number.class.isAssignableFrom(type)) {
            LOG.trace(String.format("new numeric dimension '%s/%s' added", name, type));
            d = new NumericBitMapDimension(name, dimensions.size(), type);
        } else {
            LOG.trace(String.format("new generic dimension '%s/%s' added", name, type));
            d = new BitMapDimension(name, dimensions.size(), type);
        }
        dimensions.put(name, d);

        return this;
    }
    
    @Override
    public CollectionBuilder add(Object... data) {
        CollectionBuilder b = getCollectionBuilder();
        b.add(data);
        return b;
    }

    @Override
    public CollectionBuilder getCollectionBuilder() {
        if (builder == null) {
            BitMapCollection collection = new BitMapCollection(dimensions);
            builder = new DefaultCollectionBuilder(collection);
        }
        return builder;
    }

}
