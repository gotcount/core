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
public class BasicSchemaBuilder extends SchemaBuilder {
        
    BasicSchemaBuilder() {
        // package private constructor
    }
    
    /**
     * Add a new dimension
     * 
     * @param name of the dimension, must be unique
     * @param clasz of the dimensions values
     * @return 
     */
    public BasicSchemaBuilder dimension(String name, Class clasz) {
        if (columns != null)
            throw new IllegalStateException("cannot alter dimensions when data has already been added");
        dimensions.put(name, new Dimension(name, dimensions.size(), clasz));
        return this;
    }
    
}
