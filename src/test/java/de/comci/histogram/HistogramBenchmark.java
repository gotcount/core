/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.fest.assertions.api.Assertions.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public abstract class HistogramBenchmark extends AbstractBenchmark {

    private static final int ROWS = 1000 * 1000;
    private static List<Integer> data;
    private Histogram<Integer> instance;

    abstract Histogram<Integer> getInstance();

    @BeforeClass
    public static void createData() {
        Random r = new Random();
        data = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            data.add(r.nextInt());
        }
    }

    @Before
    public void init() {
        instance = getInstance();
    }

    @Test
    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 2)
    public void simplePerformance() {
        for (int i = 0; i < ROWS; i++) {
            instance.set(i, data.get(i));
        }
    }

}
