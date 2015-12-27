/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public interface DimensionBuilder {

    public DimensionBuilder dimension(String name, Class type);
    
    public DimensionBuilder dimension(String name, Class type, Double precision);
        
    /**
     * Add a data row
     * @param data
     * @return 
     */
    public CollectionBuilder add(Object... data);
    
    public CollectionBuilder getCollectionBuilder();
    
}
