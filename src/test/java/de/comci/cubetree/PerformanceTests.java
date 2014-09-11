/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.cubetree;

import de.comci.cubetree.Filter;
import de.comci.cubetree.CubeTree;
import de.comci.cubetree.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import static org.fest.assertions.api.Assertions.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Sebastian
 */
public class PerformanceTests {
    
    private static CubeTree cube;
    
    private static final int MAX_AVERAGE_QUERY_TIME = 50;
    private final static int NUMBER_OF_TEST_RUNS = 30;
    private static final int TIME_PER_QUERY_BEFORE_TIMEOUT = 100;
    
    @BeforeClass
    public static void beforeTest() {         
        List<Dimension> dimensions = Arrays.asList(                
                new Dimension("d5", Boolean.class),
                new Dimension("d1", String.class),
                new Dimension("d2", Double.class).precision(100),
                new Dimension("d3", Short.class).precision(10),
                new Dimension("d0", Date.class).precision(Calendar.HOUR),
                new Dimension("d4", Integer.class)                
        );
        CubeTree.CubeTreeBuilder builder = CubeTree.build(dimensions);
        addData(builder, (int)(1000 * 1000 * 1), dimensions.stream().map(d -> d.clasz).collect(Collectors.toList()));
        cube = builder.done();
    }
    
    public static void addData(CubeTree.CubeTreeBuilder builder, int rows, List<Class> dimensionsOfType) {        
        
        Random r = new Random();
        
        for (int i = 0; i < rows; i++) {
            builder.add(dimensionsOfType.stream().map(c -> { 
                if (c == String.class) {
                    return "str" + ((int)(r.nextGaussian() * 60) ^ 2);
                } else if (c == Long.class || c == long.class) {
                    return (long)(r.nextGaussian() * 1000) ^ 2;
                } else if (c == Double.class || c == double.class) {
                    return r.nextGaussian() * 10000;
                } else if (c == Float.class || c == float.class) {
                    return (float)(r.nextGaussian() * 10000);
                } else if (c == Boolean.class || c == boolean.class) {
                    return r.nextBoolean();
                } else if (c == Date.class) {
                    return new Date(System.currentTimeMillis() + (long)(r.nextGaussian() * 1000 * 60 * 60 * 24 * 100));
                } else if (c == Short.class ||c == short.class) {
                    return (short)r.nextInt(Short.MAX_VALUE);
                } else { // int is the default
                    return (int)(r.nextGaussian() * 100) ^ 2;
                }
            }).toArray());
        }
    }
    
    @Test(timeout = TIME_PER_QUERY_BEFORE_TIMEOUT*NUMBER_OF_TEST_RUNS)
    public void countDateDimension() {
        List<Long> duration = new ArrayList<>(NUMBER_OF_TEST_RUNS);
        for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
            cube.count("d0");
            duration.add(cube.getLastQueryDuration());
        }
        final double avg = duration.stream().mapToLong(l -> l).average().getAsDouble();        
        System.out.println(String.format("average query duration %.1f ms", avg));
        assertThat(avg).isLessThan(MAX_AVERAGE_QUERY_TIME);
    }
    
    
    @Test(timeout = TIME_PER_QUERY_BEFORE_TIMEOUT*NUMBER_OF_TEST_RUNS)
    public void countDateDimensionWithInFilterOnStringDownInTheTree() {
        List<Long> duration = new ArrayList<>(NUMBER_OF_TEST_RUNS);
        for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
            cube.count(Arrays.asList(new Filter("d1", CubeTree.Operation.IN, "str5", "str100")), "d0");
            duration.add(cube.getLastQueryDuration());
        }
        final double avg = duration.stream().mapToLong(l -> l).average().getAsDouble();        
        System.out.println(String.format("average query duration %.1f ms", avg));
        assertThat(avg).isLessThan(MAX_AVERAGE_QUERY_TIME);
    }
    
    @Test(timeout = TIME_PER_QUERY_BEFORE_TIMEOUT*NUMBER_OF_TEST_RUNS)
    @Ignore
    public void countDateDimensionWithGTEFilterOnIntegerDimensionDownInTheTree() {
        List<Long> duration = new ArrayList<>(NUMBER_OF_TEST_RUNS);
        for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
            cube.count(Arrays.asList(new Filter("d4", CubeTree.Operation.GTE, 5)), "d0");
            duration.add(cube.getLastQueryDuration());
        }
        final double avg = duration.stream().mapToLong(l -> l).average().getAsDouble();        
        System.out.println(String.format("average query duration %.1f ms", avg));
        assertThat(avg).isLessThan(MAX_AVERAGE_QUERY_TIME);
    }
    
        
    @Test(timeout = TIME_PER_QUERY_BEFORE_TIMEOUT*NUMBER_OF_TEST_RUNS)
    public void countStringDimension() {
        List<Long> duration = new ArrayList<>(NUMBER_OF_TEST_RUNS);
        for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
            cube.count("d1");
            duration.add(cube.getLastQueryDuration());
        }
        final double avg = duration.stream().mapToLong(l -> l).average().getAsDouble();        
        System.out.println(String.format("average query duration %.1f ms", avg));
        assertThat(avg).isLessThan(MAX_AVERAGE_QUERY_TIME);
    }
    
    @Test(timeout = TIME_PER_QUERY_BEFORE_TIMEOUT*NUMBER_OF_TEST_RUNS)
    public void countDoubleDimension() {
        List<Long> duration = new ArrayList<>(NUMBER_OF_TEST_RUNS);
        for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
            cube.count("d2");
            duration.add(cube.getLastQueryDuration());
        }
        final double avg = duration.stream().mapToLong(l -> l).average().getAsDouble();        
        System.out.println(String.format("average query duration %.1f ms", avg));
        assertThat(avg).isLessThan(MAX_AVERAGE_QUERY_TIME);
    }
    
    @Test(timeout = TIME_PER_QUERY_BEFORE_TIMEOUT*NUMBER_OF_TEST_RUNS)
    public void countShortDimension() {
        List<Long> duration = new ArrayList<>(NUMBER_OF_TEST_RUNS);
        for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
            cube.count("d3");
            duration.add(cube.getLastQueryDuration());
        }
        final double avg = duration.stream().mapToLong(l -> l).average().getAsDouble();        
        System.out.println(String.format("average query duration %.1f ms", avg));
        assertThat(avg).isLessThan(MAX_AVERAGE_QUERY_TIME);
    }
    
    @Test(timeout = TIME_PER_QUERY_BEFORE_TIMEOUT*NUMBER_OF_TEST_RUNS)
    public void countIntegerDimension() {
        List<Long> duration = new ArrayList<>(NUMBER_OF_TEST_RUNS);
        for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
            cube.count("d4");
            duration.add(cube.getLastQueryDuration());
        }
        final double avg = duration.stream().mapToLong(l -> l).average().getAsDouble();        
        System.out.println(String.format("average query duration %.1f ms", avg));
        assertThat(avg).isLessThan(MAX_AVERAGE_QUERY_TIME);
    }
    
    @Test(timeout = TIME_PER_QUERY_BEFORE_TIMEOUT*NUMBER_OF_TEST_RUNS)
    public void countBooleanDimension() {
        List<Long> duration = new ArrayList<>(NUMBER_OF_TEST_RUNS);
        for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
            cube.count("d5");
            duration.add(cube.getLastQueryDuration());
        }
        final double avg = duration.stream().mapToLong(l -> l).average().getAsDouble();        
        System.out.println(String.format("average query duration %.1f ms", avg));
        assertThat(avg).isLessThan(MAX_AVERAGE_QUERY_TIME);
    }
    
}
