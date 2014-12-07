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
public interface CollectionBuilder {
    
    public CollectionBuilder add(Object... data);
    
    public BitMapCollection build();
    
}
