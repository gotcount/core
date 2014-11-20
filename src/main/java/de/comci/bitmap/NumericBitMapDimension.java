/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.util.stream.Collectors;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class NumericBitMapDimension<N extends Number> extends BitMapDimension<N> implements NumericDimension<N> {

    public NumericBitMapDimension(String name, int index, Class<N> clasz) {
        super(name, index, clasz);
    }

    @Override
    public double getExpectedValue() {
        return bitmap.entrySet()
                     .parallelStream()
                     .collect(
                        Collectors.summingDouble(e -> {
                            return e.getKey().getValue().doubleValue() 
                                * e.getValue().cardinality();
                        })) / getCardinality();
    }

    @Override
    public double getVariance() {
        final double exp = getExpectedValue();
        final long size = getCardinality();
        return bitmap.entrySet()
                     .parallelStream()
                     .collect(
                        Collectors.summingDouble(e -> {
                            return Math.pow(e.getKey().getValue().doubleValue() - exp, 2) * ((double)e.getValue().cardinality() / size);
                        }));
    }
    
}
