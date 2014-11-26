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
                            return Math.pow(e.getKey().getValue().doubleValue() - exp, 2);
                        })) / size;
    }

    @Override
    public double getCovariance(NumericDimension<N> nd) {

        final NumericBitMapDimension<N> nbd = (NumericBitMapDimension)nd;
        double expX = getExpectedValue();
        double expY = nd.getExpectedValue();

        double cov = bitmap.entrySet().parallelStream().collect(Collectors.summingDouble(outer -> {
            double x = outer.getKey().getValue().doubleValue() - expX;
            return nbd.bitmap.entrySet().parallelStream().collect(Collectors.summingDouble(inner -> {
                double y = inner.getKey().getValue().doubleValue() - expY;
                return (x*y) * outer.getValue().andCardinality(inner.getValue());
            }));
        })) / (getCardinality() - 1);

        return cov;
    }

    @Override
    public double getPearsonCorrelationCoefficient(NumericDimension<N> nd) {
        final NumericBitMapDimension<N> nbd = (NumericBitMapDimension)nd;
        double expX = getExpectedValue();
        double expY = nd.getExpectedValue();

        double sigX = getVariance() * getCardinality();
        double sigY = nd.getVariance() * getCardinality();
        
        double p = bitmap.entrySet().parallelStream().collect(Collectors.summingDouble(outer -> {
            double x = outer.getKey().getValue().doubleValue() - expX;
            return nbd.bitmap.entrySet().parallelStream().collect(Collectors.summingDouble(inner -> {
                double y = inner.getKey().getValue().doubleValue() - expY;
                return (x*y) * outer.getValue().andCardinality(inner.getValue());
            }));
        })) / Math.sqrt(sigX * sigY);

        return p;
    }

}
