/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import static org.fest.assertions.api.Assertions.*;
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
    
}
