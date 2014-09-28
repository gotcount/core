/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
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
public class BitMapCollectionTest {

    private GotCountDB instance;

    private final Comparator<Dimension> dimensionComparator = new Comparator<Dimension>() {

        @Override
        public int compare(Dimension o1, Dimension o2) {

            int c = o1.getName().compareTo(o2.getName());

            if (c == 0) {
                c = (o1.getType().getName()).compareTo(o2.getType().getName());
            }

            return c;
        }

    };

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        instance = GotCountDB.create()
                .dimension("d0", String.class)
                .dimension("d1", Integer.class)
                .get();
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
        instance = GotCountDB.create()
                .dimension("d0", String.class)
                .dimension("d1", Integer.class)
                .get();

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
    public void rowSize() {
        instance.build();
        assertThat(instance.size()).isEqualTo(8);
    }

    private static class SimpleTestDimension<T> implements Dimension<T> {

        final String name;
        final Class type;

        public SimpleTestDimension(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<T> getType() {
            return type;
        }

    }

    @Test
    public void getNonExistingDimension() {

        try {
            instance.getDimension("0");
            fail("missing exception");
        } catch (NoSuchElementException e) {
            assertThat(e.getMessage()).isEqualTo("no dimension with name '0' exists");
        }
    }

    @Test
    public void getExistingDimension() {

        assertThat(instance.getDimension("d0")).usingComparator(dimensionComparator).isEqualTo(new SimpleTestDimension<>("d0", String.class));
    }

    @Test
    public void listDimensions() {

        assertThat(instance.getDimensions()).usingElementComparator(dimensionComparator).containsOnly(
                new SimpleTestDimension<>("d0", String.class),
                new SimpleTestDimension<>("d1", Integer.class)
        );
    }

    @Test
    public void getHistogramForDimension() {

        instance.build();

        Multiset<Value> d0map = HashMultiset.create();
        d0map.add(Value.get("123"), 2);
        d0map.add(Value.empty(String.class), 1);
        d0map.add(Value.get(""), 1);
        d0map.add(Value.get("-1"), 1);
        d0map.add(Value.get("3"), 3);

        assertThat(instance.histogram("d0")).isEqualTo(d0map);
    }

    @Test
    public void getHistogramForDimensionTop2() {

        instance.build();

        Multiset<Value> d0map = HashMultiset.create();
        d0map.add(Value.get("3"), 3);
        d0map.add(Value.get("123"), 2);
        final Multiset<Value> histogram = instance.histogram("d0", 2);

        assertThat(histogram).isEqualTo(d0map);
    }

    @Test
    public void getHistogramForDimensionBottom3() {

        instance.build();

        Multiset<Value> d0map = HashMultiset.create();
        d0map.add(Value.empty(String.class), 1);
        d0map.add(Value.get(""), 1);
        d0map.add(Value.get("-1"), 1);
        final Multiset<Value> histogram = instance.histogram("d0", -3);

        assertThat(histogram).isEqualTo(d0map);
    }

    @Test
    public void histogramWithFiltersEquals1() {

        instance.build();

        Multiset<Value> d0map = HashMultiset.create();
        d0map.add(Value.get("123"), 0);
        d0map.add(Value.empty(String.class), 1);
        d0map.add(Value.get(""), 0);
        d0map.add(Value.get("-1"), 0);
        d0map.add(Value.get("3"), 1);

        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d1", v -> (int) v == 1);

        assertThat(instance.histogram("d0", filter)).isEqualTo(d0map);
    }

    @Test
    public void histogramWithFiltersLessThen0() {

        instance.build();

        Multiset<Value> d0map = HashMultiset.create();
        d0map.add(Value.get("123"), 0);
        d0map.add(Value.empty(String.class), 0);
        d0map.add(Value.get(""), 0);
        d0map.add(Value.get("-1"), 1);
        d0map.add(Value.get("3"), 1);

        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d1", v -> (int) v < 0);

        assertThat(instance.histogram("d0", filter)).isEqualTo(d0map);
    }

    @Test
    public void histogramWithFiltersGreaterThen0() {

        instance.build();

        Multiset<Value> d0map = HashMultiset.create();
        d0map.add(Value.get("123"), 2);
        d0map.add(Value.empty(String.class), 1);
        d0map.add(Value.get(""), 0);
        d0map.add(Value.get("-1"), 0);
        d0map.add(Value.get("3"), 2);

        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d1", v -> (int) v > 0);

        assertThat(filter.get("d1").test(0)).isFalse();
        assertThat(filter.get("d1").test(1)).isTrue();

        assertThat(instance.histogram("d0", filter)).isEqualTo(d0map);
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
    public void addAfterBuiltWithoutAdditionalBuild() {

        instance.build();

        try {
            instance.add(new Object[]{"123", 123});
            instance.count("d0", "123");
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("must build before querying");
        }
    }

    @Test//(timeout = 200)
    public void sizeTest1k() {
        sizeTestN(1000);
    }

    @Test(timeout = 200)
    public void sizeTest10k() {
        sizeTestN(1000 * 10);
    }

    @Test(timeout = 400)
    public void sizeTest100k() {
        sizeTestN(1000 * 100);
    }

    //@Test(timeout = 2000)
    public void sizeTest1m() {
        sizeTestN(1000 * 1000);
    }

    //@Test(timeout = 15000)
    public void sizeTest10m() {
        sizeTestN(1000 * 1000 * 10);
    }

    private void sizeTestN(int size) {
        instance = GotCountDB.create()
                .dimension("d0", String.class)
                .dimension("d1", String.class)
                .dimension("d2", String.class)
                .dimension("d3", Integer.class)
                .dimension("d4", Long.class)
                .dimension("d5", String.class)
                .dimension("d6", String.class)
                .dimension("d7", String.class)
                .dimension("d8", String.class)
                .get();

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
        filter.put("d3", v -> (int) v > (int) td[3].values[2]);
        assertThat(instance.histogram("d0", filter).elementSet()).isEqualTo(td[0].getHistogram().elementSet());

        // test histogram values
        for (int i = 0; i < 7; i++) {
            assertThat(instance.histogram("d" + i)).isEqualTo(td[i].getHistogram());
        }

        // multi filter
        // currently not working as multimap does not allow values with a zero count
        //filter = new HashMap<>();
        //filter.put("d3", v -> (int) v > (int) td[3].values[2]);
        //filter.put("d4", v -> (long) v > 100l && (long) v < 1000l);
        //filter.put("d6", v -> v.equals(td[6].values[1]));
        //assertThat(instance.histogram("d0", filter).elementSet()).isEqualTo(td[0].getHistogram().elementSet());
    }

    static class TestDimension<T> implements Dimension<T> {

        private final Multiset<T> histogram = HashMultiset.create();
        private T[] values;
        private final Random r = new Random();

        Multiset<Value> getHistogram() {
            Multiset<Value> multiset = HashMultiset.create();
            for (T t : histogram.elementSet()) {
                multiset.add(Value.get(t), histogram.count(t));
            }
            return multiset;
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

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Class<T> getType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

}
