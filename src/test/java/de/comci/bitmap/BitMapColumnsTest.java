/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import static org.fest.assertions.api.Assertions.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class BitMapColumnsTest {

    private BitMapColumns instance;

    public BitMapColumnsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        instance = new BitMapColumns();
        instance.dimension("d0", String.class);
        instance.dimension("d1", Integer.class);

        instance.add(new Object[]{"123", 123});
        instance.add(new Object[]{null, 1});
        instance.add(new Object[]{"", 0});
        instance.add(new Object[]{"-1", -1});
        instance.add(new Object[]{"123", 5});
        instance.add(new Object[]{"3", 123});
        instance.add(new Object[]{"3", 1});
        instance.add(new Object[]{"3", -1});
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of dimension method, of class BitMapColumns.
     */
    @Test
    public void countSingleValueInDimension() {
        BitMapColumns instance = new BitMapColumns();
        instance.dimension("d0", String.class);
        instance.dimension("d1", Integer.class);

        instance.add(new Object[]{"123", 123});
        instance.add(new Object[]{null, 1});
        instance.add(new Object[]{"", 0});
        instance.add(new Object[]{"-1", -1});
        instance.add(new Object[]{"123", 5});
        instance.add(new Object[]{"3", 123});

        instance.build();

        assertThat(instance.count("d0", "123")).isEqualTo(2);
        assertThat(instance.count("d0", "3")).isEqualTo(1);
        assertThat(instance.count("d0", "")).isEqualTo(1);
        assertThat(instance.count("d0", null)).isEqualTo(1);
        assertThat(instance.count("d1", null)).isEqualTo(0);
    }

    @Test
    public void getHistogramForDimension() {

        instance.build();

        Map<String, Integer> d0map = new HashMap<>();
        d0map.put("123", 2);
        d0map.put(null, 1);
        d0map.put("", 1);
        d0map.put("-1", 1);
        d0map.put("3", 3);
        
        assertThat(instance.<String>histogram("d0")).isEqualTo(d0map);
    }

    @Test
    public void histogramWithFiltersEquals1() {

        instance.build();
        
        Map<String, Integer> d0map = new HashMap<>();
        d0map.put("123", 0);
        d0map.put(null, 1);
        d0map.put("", 0);
        d0map.put("-1", 0);
        d0map.put("3", 1);

        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d1", v -> (int) v == 1);

        assertThat(instance.<String>histogram("d0", filter)).isEqualTo(d0map);
    }

    @Test
    public void histogramWithFiltersLessThen0() {

        instance.build();

        Map<String, Integer> d0map = new HashMap<>();
        d0map.put("123", 0);
        d0map.put(null, 0);
        d0map.put("", 0);
        d0map.put("-1", 1);
        d0map.put("3", 1);
        
        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d1", v -> (int) v < 0);

        assertThat(instance.<String>histogram("d0", filter)).isEqualTo(d0map);
    }

    @Test
    public void histogramWithFiltersGreaterThen0() {

        instance.build();

        Map<String, Integer> d0map = new HashMap<>();
        d0map.put("123", 2);
        d0map.put(null, 1);
        d0map.put("", 0);
        d0map.put("-1", 0);
        d0map.put("3", 2);

        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d1", v -> (int) v > 0);

        assertThat(filter.get("d1").test(0)).isFalse();
        assertThat(filter.get("d1").test(1)).isTrue();

        assertThat(instance.<String>histogram("d0", filter)).isEqualTo(d0map);
    }

    @Test
    public void histogramWithItselfInFilter() {

        instance.build();

        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d0", v -> v.equals("123"));

        try {
            instance.<String>histogram("d0", filter);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("cannot filter on histogram dimension");
        }
    }

    @Test
    public void histogramWithoutBuild() {

        try {
            instance.<String>histogram("d0");
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("must build before querying");
        }
    }

    @Test
    public void countWithoutBuild() {

        try {
            instance.count("d0", "123");
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("must build before querying");
        }
    }

    @Test
    public void addAfterBuilt() {

        instance.build();

        try {
            instance.add(new Object[]{"123", 123});
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("already built");
        }
    }

    @Test(timeout = 200)
    public void sizeTest1k() {
        sizeTestN(1000);
    }
    
    @Test(timeout = 200)
    public void sizeTest10k() {
        sizeTestN(1000*10);
    }
    
    @Test(timeout = 400)
    public void sizeTest100k() {
        sizeTestN(1000*100);
    }
    
    @Test(timeout = 2000)
    public void sizeTest1m() {
        sizeTestN(1000*1000);
    }
   
    @Test(timeout = 15000)
    public void sizeTest10m() {
        sizeTestN(1000*1000*10);
    }

    private void sizeTestN(int size) {
        instance = new BitMapColumns();
        instance.dimension("d0", String.class);
        instance.dimension("d1", String.class);
        instance.dimension("d2", String.class);
        instance.dimension("d3", Integer.class);
        instance.dimension("d4", Long.class);
        instance.dimension("d5", String.class);
        instance.dimension("d6", String.class);
        instance.dimension("d7", String.class);
        instance.dimension("d8", String.class);

        TestDimension[] td = new TestDimension[]{
            TestDimension.withStrings(10),
            TestDimension.withStrings(10),
            TestDimension.withStrings(10),
            TestDimension.withIntegers(150),
            TestDimension.withLongs(150),
            TestDimension.withStrings(5),
            TestDimension.withStrings(10),
            TestDimension.withStrings(200),
            TestDimension.withStrings(1000)
        };
        
        for (int i = 0; i < size; i++) {            
            Object[] data = new Object[td.length];
            for (int d = 0; d < data.length; d++) {                
                data[d] = td[d].get();
            }
            instance.add(data);
        }
        
        instance.build();
        
        // all values in histogram
        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d3", v -> (int)v > (int)td[3].values[2]);        
        assertThat(instance.histogram("d0", filter).keySet()).containsOnly(td[0].histogram.elementSet().toArray());
        
        // test histogram values
        for (int i = 0; i < 7; i++) {
            assertThat(instance.histogram("d" + i)).isEqualTo(td[i].getHistogram());            
        }
        
        // multi filter
        filter = new HashMap<>();
        filter.put("d3", v -> (int)v > (int)td[3].values[2]);
        filter.put("d4", v -> (long)v > 100l && (long)v < 1000l);
        filter.put("d6", v -> v.equals(td[6].values[1]));
        assertThat(instance.histogram("d0", filter).keySet()).containsOnly(td[0].histogram.elementSet().toArray());
        
    }

    static class TestDimension<T> {

        private final Multiset<T> histogram = HashMultiset.create();
        private T[] values;
        private final Random r = new Random();

        Map<T, Integer> getHistogram() {
            Map<T, Integer> map = new HashMap<>();
            for (T t : histogram.elementSet()) {
                map.compute(t, (k,v) -> (int)histogram.count(k));
            }
            return map;
        }
        
        static TestDimension withStrings(int uniqueValues) {
            TestDimension<String> t = new TestDimension<>();
            t.values = new String[uniqueValues];
            for (int i = 0; i < uniqueValues; i++) {
                final double g = t.r.nextGaussian();
                t.values[i] = "" + Math.floor(Math.sqrt(g * g) * 10);
            }
            return t;
        }

        static TestDimension withIntegers(int uniqueValues) {
            TestDimension<Integer> t = new TestDimension<>();
            t.values = new Integer[uniqueValues];
            for (int i = 0; i < uniqueValues; i++) {
                t.values[i] = t.r.nextInt();
            }
            return t;
        }

        static TestDimension withLongs(int uniqueValues) {
            TestDimension<Long> t = new TestDimension<>();
            t.values = new Long[uniqueValues];
            for (int i = 0; i < uniqueValues; i++) {
                t.values[i] = t.r.nextLong();
            }
            return t;
        }

        T get() {
            final T val = values[r.nextInt(values.length)];
            histogram.add(val);
            return val;
        }

    }

}
