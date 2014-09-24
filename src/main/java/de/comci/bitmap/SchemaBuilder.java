/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class SchemaBuilder {
    
    protected BitMapCollection columns;
    protected final Map<String, BitMapDimension> dimensions = new HashMap<>();
    private final List<Object[]> data = new ArrayList<>(1000);

    public SchemaBuilder add(Object[] tuple) {
        if (columns != null)
            throw new IllegalStateException("already build");
        this.data.add(tuple);
        return this;
    }

    public BitMapCollection get() {
        if (columns == null) {
            columns = new BitMapCollection(dimensions);
            columns.add(data);
            columns.build();
        }
        return columns;
    }
    
    int size() {
        return data.size();
    }
    
}
