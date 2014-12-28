/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import static org.fest.assertions.api.Assertions.assertThat;
import org.fest.assertions.data.Offset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class NumericBitMapDimensionTest {

    public NumericBitMapDimensionTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void expectedValueIntDice() {
        NumericBitMapDimension<Integer> instance = new NumericBitMapDimension<>("test", 0, Integer.class);
        instance.set(0, 1)
                .set(1, 2)
                .set(2, 3)
                .set(3, 4)
                .set(4, 5)
                .set(5, 6);
        assertThat(instance.getExpectedValue()).isEqualTo(3.5);
    }

    @Test
    public void varianceIntDice() {
        NumericBitMapDimension<Integer> instance = new NumericBitMapDimension<>("test", 0, Integer.class);
        instance.set(0, 1)
                .set(1, 2)
                .set(2, 3)
                .set(3, 4)
                .set(4, 5)
                .set(5, 6);
        assertThat(instance.getVariance()).isEqualTo(2.9166, Offset.offset(.0001));
    }

    @Test
    public void covarianceFullyNegative() {

        NumericBitMapDimension<Integer> x = new NumericBitMapDimension<>("X", 0, Integer.class);
        x.set(0, 1).set(1, 2).set(2, 3).set(3, 4);
        NumericBitMapDimension<Integer> y = new NumericBitMapDimension<>("Y", 0, Integer.class);
        y.set(0, 4).set(1, 3).set(2, 2).set(3, 1);

        assertThat(x.getCovariance(y)).isEqualTo(-1.66666, Offset.offset(0.00001));
        assertThat(x.getCovariance(y)).isEqualTo(y.getCovariance(x));

    }
    
    @Test
    public void covarianceFullyPositive() {

        NumericBitMapDimension<Integer> x = new NumericBitMapDimension<>("X", 0, Integer.class);
        x.set(0, 1).set(1, 2).set(2, 3).set(3, 4);
        NumericBitMapDimension<Integer> y = new NumericBitMapDimension<>("Y", 0, Integer.class);
        y.set(0, 1).set(1, 2).set(2, 3).set(3, 4);

        assertThat(x.getCovariance(y)).isEqualTo(1.66666, Offset.offset(0.00001));
        assertThat(x.getCovariance(y)).isEqualTo(y.getCovariance(x));

    }

    @Test
    public void pearsonCorrelationCoefficientFullyNegative() {

        NumericBitMapDimension<Integer> x = new NumericBitMapDimension<>("X", 0, Integer.class);
        x.set(0, 1).set(1, 2).set(2, 3).set(3, 4);
        NumericBitMapDimension<Integer> y = new NumericBitMapDimension<>("Y", 0, Integer.class);
        y.set(0, 4).set(1, 3).set(2, 2).set(3, 1);

        assertThat(x.getPearsonCorrelationCoefficient(y)).isEqualTo(-1);
        assertThat(x.getPearsonCorrelationCoefficient(y)).isEqualTo(y.getPearsonCorrelationCoefficient(x));

    }

    @Test
    public void pearsonCorrelationCoefficientFullyPositive() {

        NumericBitMapDimension<Integer> x = new NumericBitMapDimension<>("X", 0, Integer.class);
        x.set(0, 1).set(1, 2).set(2, 3).set(3, 4);
        NumericBitMapDimension<Integer> y = new NumericBitMapDimension<>("Y", 0, Integer.class);
        y.set(0, 1).set(1, 2).set(2, 3).set(3, 4);

        assertThat(x.getPearsonCorrelationCoefficient(y)).isEqualTo(1);
        assertThat(x.getPearsonCorrelationCoefficient(y)).isEqualTo(y.getPearsonCorrelationCoefficient(x));
    }

}
