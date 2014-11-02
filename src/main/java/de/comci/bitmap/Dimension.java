/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.bitmap;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 * @param <T>
 */
public interface Dimension<T> {

    String getName();
    
    Class<T> getType();
    
    long getCardinality();
    
}
