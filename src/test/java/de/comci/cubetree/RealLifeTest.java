/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.cubetree;

import de.comci.cubetree.Filter;
import de.comci.cubetree.CubeTree;
import de.comci.cubetree.Dimension;
import de.comci.parser.apache.ApacheLogRecord;
import de.comci.parser.apache.ApacheParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class RealLifeTest {

    private static final int NUM_LINES = 100000;
    private static CubeTree cube;

    @BeforeClass
    public static void beforeClass() throws FileNotFoundException {
        CubeTree.CubeTreeBuilder b = CubeTree.build(
                new Dimension("date", Date.class).precision(Calendar.HOUR),
                new Dimension("status", Integer.class),
                new Dimension("method", String.class),
                new Dimension("requestType", String.class),
                new Dimension("request", String.class),
                new Dimension("bytes", Integer.class).buckets(true, 100d, 1000d, 10000d, 100000d, 1000000d),
                new Dimension("client", String.class));

        Stream<ApacheLogRecord> log = ApacheParser.parse(new File("D:\\Downloads\\NASA_access_log_Aug95\\access_log_Aug95"));
        log.limit(NUM_LINES).forEach(l -> b.add(l.date, l.statusCode, l.method, l.getRequestType(), l.request, l.bytesSent, l.client));
        cube = b.done();
    }

    @Test
    public void clientCountByStatusMethodDateRange() {

        cube.count(Arrays.asList(
                new Filter("status", CubeTree.Operation.IN, 200, 201),
                new Filter("method", CubeTree.Operation.EQ, "GET"),
                new Filter("date", CubeTree.Operation.BETWEEN, new Date(95, 7, 1), new Date(95, 7, 4))
        ), 10, "client");

    }
    
    @Test
    public void clientCountByStatusMethod() {

        cube.count(Arrays.asList(
                new Filter("status", CubeTree.Operation.IN, 200, 201),
                new Filter("method", CubeTree.Operation.EQ, "GET")
        ), 10, "client");

    }

    @Test
    public void dateCount() {
        CubeTree.ResultNode count = cube.count(1000, "date");
        System.out.println(count);
    }
    
    @Test
    public void requestTypeCount() {

        CubeTree.ResultNode count = cube.count(1000, "requestType");
        System.out.println(count);

    }

}
