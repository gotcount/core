/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import com.carrotsearch.junitbenchmarks.annotation.LabelType;
import de.comci.histogram.Histogram;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "benchmark-lists")
@BenchmarkHistoryChart(labelWith = LabelType.TIMESTAMP, maxRuns = 20)
public class HighCardinalityBenchmark extends AbstractBenchmark {

    static BitMapCollection instance;
    static final int rows = 1 * 200 * 1000;

    @BeforeClass
    public static void startup() {

        System.out.println("creating benchmark data");

        CollectionBuilder builder = BitMapCollection.builder()
                .dimension("d0", Integer.class)
                .dimension("d1", Integer.class)
                .getCollectionBuilder();

        for (int i = 0; i < rows; i++) {
            builder.add(i, i % 1000);
        }
        instance = builder.build();
    }
    
    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void withoutFilter() {

        assertThat(instance.count("d0", 1)).isEqualTo(1);
        assertThat(instance.size()).isEqualTo(rows);

        final Histogram<Value> histogram = instance.histogram("d0").setLimit(10).build();
        //assertThat(histogram.size()).isEqualTo(10);
        
    }

    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void withEqualsFilterOnIntegerColumn() {

        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d1", p -> (int) p == 5);

        assertThat(instance.count("d0", 1)).isEqualTo(1);
        assertThat(instance.size()).isEqualTo(rows);

        final Histogram<Value> histogram = instance.histogram("d0").setFilters(filter).setLimit(10).build();
        //assertThat(histogram.size()).isEqualTo(10);

    }
    
    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void withRangeFilterOnIntegerColumn() {

        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d1", p -> (int) p > 5 && (int)p < 7);

        assertThat(instance.count("d0", 1)).isEqualTo(1);
        assertThat(instance.size()).isEqualTo(rows);

        final Histogram<Value> histogram = instance.histogram("d0").setFilters(filter).setLimit(10).build();
        //assertThat(histogram.size()).isEqualTo(10);

    }

}
