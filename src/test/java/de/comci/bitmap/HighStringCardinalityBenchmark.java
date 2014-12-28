/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import de.comci.histogram.Histogram;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.lang.RandomStringUtils;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class HighStringCardinalityBenchmark extends AbstractBenchmark {

    static BitMapCollection instance;
    static final int rows = 2 * 1000 * 1000;
    static final int rndString = 1000 * 100 * 2;
    
    @BeforeClass
    public static void startup() {

        System.out.println("creating benchmark data");

        List<String> rndStrings = new ArrayList<>(rndString);
        for (int i = 0; i < rndString; i++) {
            rndStrings.add(RandomStringUtils.random(20));
        }
        
        CollectionBuilder builder = BitMapCollection.builder()
                .dimension("d0", String.class)
                .dimension("d1", Integer.class)
                .getCollectionBuilder();

        for (int i = 0; i < rows; i++) {
            builder.add(rndStrings.get(i % rndString), i % 1000);
        }
        known = "123.456.789.012";
        builder.add(known, 1000);
        builder.add(known, 1000);
        builder.add(known, 1000);
        builder.add(known, 1000);

        instance = builder.build();
    }
    private static String known;

    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void withoutFilter() {
        assertThat(instance.size()).isEqualTo(rows + 4);
        assertThat(instance.count("d0", known)).isEqualTo(4);
        assertThat(instance.count("d1", 1000)).isEqualTo(4);
    }

    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    public void histogramWithFilter() {
        Map<String, Predicate> filter = new HashMap<>();
        filter.put("d0", p -> p.equals(known));
        Histogram<Value> histogram = instance.histogram("d0").setFilters(filter).setLimit(50).build();
        assertThat(histogram.keySet()).containsOnly(Value.get(known));
    }

}
