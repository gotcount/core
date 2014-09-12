/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.bitmap;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
class SchemaBuilder {
    protected BitMapColumns columns;
    protected final Map<String, Dimension> dimensions = new HashMap<>();

    public BitMapColumns add(Object[] tuple) {
        return get().add(tuple);
    }

    public BitMapColumns get() {
        if (columns == null) {
            columns = new BitMapColumns(dimensions);
        }
        return columns;
    }
    
}
