/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import de.comci.histogram.HashHistogram;
import de.comci.histogram.Histogram;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.fest.assertions.api.Assertions.*;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class BitMapDimensionTest {

    public BitMapDimensionTest() {
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
    public void histogramWithFilterRemovingWrongValues() {

        BitMapDimension<Integer> d = new BitMapDimension("test", 0, Integer.class);
        add(d, 4, 17); // %3 = 1
        add(d, 5, 16); // %3 = 2
        add(d, 1, 13); // %3 = 1
        add(d, 2, 12); // %3 = 2
        add(d, 3, 22); // %3 = 0
        add(d, 7, 11); // %3 = 1
        add(d, 8, 11); // %3 = 2
        add(d, 10, 15); // %3 = 1
        d.build();
        
        final EWAHCompressedBitmap filter = d.filter(p -> (int)p % 3 != 0);
        Histogram<Value<Integer>> actual = d.histogram(filter, 4);
        
        Histogram<Value<Integer>> expected = new HashHistogram<>();
        expected.set(new Value<>(4, Integer.class), 17);
        expected.set(new Value<>(5, Integer.class), 16);
        expected.set(new Value<>(10, Integer.class), 15);
        expected.set(new Value<>(1, Integer.class), 13);
        
        assertThat(actual).isEqualTo(expected);

    }

    private <T> void add(BitMapDimension<T> d, T value, int count) {
        final int size = d.sortedMaps.stream().mapToInt(e -> e.getValue().cardinality()).sum();
        for (int row = size; row < count + size; row++) {
            d.set(row, value);
        }
    }
    
    public void testGetSimple() {
        
        BitMapDimension<String> d = new BitMapDimension<>("test", 0, String.class);
        d.set(0, "0");
        d.set(1, "1");
        d.set(2, "2");
        d.set(3, "3");
        d.set(4, "4");
        d.set(5, "5");
        
        assertThat(d.get(0)).isEqualTo("0");
        assertThat(d.get(1)).isEqualTo("1");
        assertThat(d.get(2)).isEqualTo("2");
        assertThat(d.get(3)).isEqualTo("3");
        assertThat(d.get(4)).isEqualTo("4");
        assertThat(d.get(5)).isEqualTo("5");
        
    }
    
    public void testGetReversed() {
        
        BitMapDimension<String> d = new BitMapDimension<>("test", 0, String.class);
        d.set(0, "5");
        d.set(1, "4");
        d.set(2, "3");
        d.set(3, "2");
        d.set(4, "1");
        d.set(5, "0");
        
        assertThat(d.get(0)).isEqualTo("5");
        assertThat(d.get(1)).isEqualTo("4");
        assertThat(d.get(2)).isEqualTo("3");
        assertThat(d.get(3)).isEqualTo("2");
        assertThat(d.get(4)).isEqualTo("1");
        assertThat(d.get(5)).isEqualTo("0");
        
    }

}
