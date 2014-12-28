/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import de.comci.histogram.Histogram;
import de.comci.histogram.LimitedHashHistogram;
import static org.fest.assertions.api.Assertions.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class BitMapCollectionBucketsTest {

    private BitMapCollection instance;

    @Before
    public void setUp() {
        instance = BitMapCollection.builder()
                .dimension("d0", Integer.class)
                .dimension("d1", String.class)
                .add(new Object[]{0, "a"})
                .add(new Object[]{1, "a"})
                .add(new Object[]{2, "a"})
                .add(new Object[]{3, "a"})
                .add(new Object[]{4, "a"})
                .add(new Object[]{0, "b"})
                .add(new Object[]{1, "b"})
                .add(new Object[]{2, "b"})
                .add(new Object[]{3, "b"})
                .add(new Object[]{4, "b"})
                .add(new Object[]{0, "c"})
                .add(new Object[]{1, "c"})
                .add(new Object[]{2, "d"})
                .add(new Object[]{3, "d"})
                .add(new Object[]{4, "d"})
                .build();
    }

    @Test
    public void shouldCreateAHistogramWithTwoBucketsNoFilter() {

        final Value<Integer> bucket0 = Value.bucket(Integer.class, "multiples of 2");
        final Value<Integer> bucket1 = Value.bucket(Integer.class, "all others");

        Histogram<Value> actual = instance.histogram("d0")
                .bucket(bucket0, p -> (int) p % 2 == 0)
                .bucket(bucket1, p -> (int) p % 2 != 0)
                .build();

        Histogram expected = new LimitedHashHistogram();
        expected.set(bucket0, 9);
        expected.set(bucket1, 6);

        assertThat(actual).isEqualTo(expected);

    }

    @Test
    public void shouldCreateAHistogramWithTwoBucketsAndFilter() {
        
        final Value<Integer> bucket0 = Value.bucket(Integer.class, "multiples of 2");
        final Value<Integer> bucket1 = Value.bucket(Integer.class, "all others");

        Histogram<Value> actual = instance.histogram("d0")
                .bucket(bucket0, p -> (int) p % 2 == 0)
                .bucket(bucket1, p -> (int) p % 2 != 0)
                .filter("d1", p -> p.equals("b"))
                .build();

        Histogram expected = new LimitedHashHistogram();
        expected.set(bucket0, 3);
        expected.set(bucket1, 2);

        assertThat(actual).isEqualTo(expected);

    }
    
    @Test
    public void shouldCreateAHistogramWithTwoBucketsAndFilterOnHistogramDimension() {
        
        final Value<Integer> bucket0 = Value.bucket(Integer.class, "multiples of 2");
        final Value<Integer> bucket1 = Value.bucket(Integer.class, "all others");

        Histogram<Value> actual = instance.histogram("d0")
                .bucket(bucket0, p -> (int) p % 2 == 0)
                .bucket(bucket1, p -> (int) p % 2 != 0)
                .filter("d0", p -> (int)p != 2)
                .filter("d1", p -> p.equals("b"))
                .build();

        Histogram expected = new LimitedHashHistogram();
        expected.set(bucket0, 2); // 0,4
        expected.set(bucket1, 2); // 1,3

        assertThat(actual).isEqualTo(expected);

    }

}
