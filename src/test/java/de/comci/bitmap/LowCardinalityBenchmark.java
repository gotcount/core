/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import de.comci.histogram.Histogram;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class LowCardinalityBenchmark extends AbstractBenchmark {

    static BitMapCollection instance;
    static final int rows = 10 * 1000 * 1000;

    @BeforeClass
    public static void startup() {

        System.out.println("creating benchmark data");

        CollectionBuilder builder = BitMapCollection.builder()
                .dimension("d0", Integer.class)
                .dimension("d1", Integer.class)
                .getCollectionBuilder();

        for (int i = 0; i < rows; i++) {
            builder.add(i % 10, i % 100);
        }
        instance = builder.build();
    }
    
    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void withoutFilterTopRange() {

        assertThat(instance.count("d0", 1)).isEqualTo(rows / 10);
        assertThat(instance.size()).isEqualTo(rows);

        final Histogram<Value> histogram = instance.histogram("d0").setLimit(10).build();
        assertThat(histogram.size()).isEqualTo(10);
        
    }
    
    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void withoutFilterBottomRange() {

        assertThat(instance.count("d0", 1)).isEqualTo(rows / 10);
        assertThat(instance.size()).isEqualTo(rows);

        final Histogram<Value> histogram = instance.histogram("d0").setLimit(-10).build();
        assertThat(histogram.size()).isEqualTo(10);
        
    }

    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void withFilterTopRange() {
        withFilter(10);
    }
    
    @Test
    @Ignore
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void withFilterBottomRange() {
        withFilter(-5); // strange bug here
    }

    private void withFilter(int range) {
        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d0", p -> (int) p == 5);

        assertThat(instance.count("d0", 1)).isEqualTo(rows / 10);
        assertThat(instance.size()).isEqualTo(rows);

        final Histogram<Value> histogram = instance.histogram("d1").setFilters(filter).setLimit(range).build();
        assertThat(histogram.size()).isEqualTo(Math.abs(range));
    }

}
