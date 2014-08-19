/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.parser.apache;

import de.comci.parser.apache.ApacheParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.nashorn.internal.runtime.arrays.ArrayLikeIterator;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Sebastian
 */
public class ApacheSessionParserTest {
    
    public ApacheSessionParserTest() {
    }
   
    private static class Testdatum {        
        String input;
        ExtendedApacheLogRecord output; 

        public Testdatum(String input, ExtendedApacheLogRecord output) {
            this.input = input;
            this.output = output;
        }
        
    }
    
    private static List<Testdatum> tests = new ArrayList<>();
    
    static {
        
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT-4:00"));
        c.set(Calendar.MILLISECOND, 0); // fucking date/time handling in java
        
        c.set(1995, 7, 1, 0, 0, 1);                        
        tests.add(new Testdatum(
                    "in24.inetnebr.com - - [01/Aug/1995:00:00:01 -0400] \"GET /shuttle/missions/sts-68/news/sts-68-mcc-05.txt HTTP/1.0\" 200 1839",
                    new ExtendedApacheLogRecord("in24.inetnebr.com", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/missions/sts-68/news/sts-68-mcc-05.txt", 
                            "HTTP/1.0", (short)200, 1839, null, null, "19950801-1", ExtendedApacheLogRecord.SessionState.SINGLE)));
        
        c.set(1995, 7, 1, 0, 0, 51);
        tests.add(new Testdatum(
                    "133.43.96.45 - - [01/Aug/1995:00:00:51 -0400] \"GET /shuttle/resources/orbiters/orbiters-logo.gif HTTP/1.0\" 200 1932",
                    new ExtendedApacheLogRecord("133.43.96.45", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/resources/orbiters/orbiters-logo.gif", 
                            "HTTP/1.0", (short)200, 1932, null, null, "19950801-2", ExtendedApacheLogRecord.SessionState.SINGLE)));
        
        c.set(1995, 7, 1, 0, 2, 13);
        tests.add(new Testdatum(
                    "piweba1y.prodigy.com - - [01/Aug/1995:00:02:13 -0400] \"GET / HTTP/1.0\" 200 7280",
                    new ExtendedApacheLogRecord("piweba1y.prodigy.com", null, 
                            null, c.getTime(), "GET", 
                            "/", 
                            "HTTP/1.0", (short)200, 7280, null, null, "19950801-3", ExtendedApacheLogRecord.SessionState.SINGLE)));
        
        c.set(1995, 7, 1, 0, 3, 53);
        tests.add(new Testdatum(
                    "gw1.att.com - - [01/Aug/1995:00:03:53 -0400] \"GET /shuttle/missions/sts-73/news HTTP/1.0\" 302 -",
                    new ExtendedApacheLogRecord("gw1.att.com", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/missions/sts-73/news", 
                            "HTTP/1.0", (short)302, -1, null, null, "19950801-4", ExtendedApacheLogRecord.SessionState.START)));
        
        c.set(1995, 7, 1, 0, 6, 53);
        tests.add(new Testdatum(
                    "gw1.att.com - - [01/Aug/1995:00:06:53 -0400] \"GET /shuttle/missions/sts-73/news HTTP/1.0\" 302 -",
                    new ExtendedApacheLogRecord("gw1.att.com", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/missions/sts-73/news", 
                            "HTTP/1.0", (short)302, -1, null, null, "19950801-4", ExtendedApacheLogRecord.SessionState.DURING)));
        
        c.set(1995, 7, 1, 0, 15, 53);
        tests.add(new Testdatum(
                    "gw1.att.com - - [01/Aug/1995:00:15:53 -0400] \"GET /shuttle/missions/sts-73/news HTTP/1.0\" 302 -",
                    new ExtendedApacheLogRecord("gw1.att.com", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/missions/sts-73/news", 
                            "HTTP/1.0", (short)302, -1, null, null, "19950801-4", ExtendedApacheLogRecord.SessionState.END)));
        
        c.set(1995, 7, 1, 0, 45, 54);
        tests.add(new Testdatum(
                    "gw1.att.com - - [01/Aug/1995:00:45:54 -0400] \"GET /shuttle/missions/sts-73/news HTTP/1.0\" 302 -",
                    new ExtendedApacheLogRecord("gw1.att.com", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/missions/sts-73/news", 
                            "HTTP/1.0", (short)302, -1, null, null, "19950801-5", ExtendedApacheLogRecord.SessionState.SINGLE)));
        
        c.set(1995, 7, 1, 0, 12, 37);
        tests.add(new Testdatum(
                    "pipe1.nyc.pipeline.com - - [01/Aug/1995:00:12:37 -0400] \"GET /history/apollo/apollo-13/apollo-13-patch-small.gif\" 200 12859",
                    new ExtendedApacheLogRecord("pipe1.nyc.pipeline.com", null, 
                            null, c.getTime(), "GET", 
                            "/history/apollo/apollo-13/apollo-13-patch-small.gif", 
                            null, (short)200, 12859, null, null, "19950801-6", ExtendedApacheLogRecord.SessionState.SINGLE)));
        
        c.set(1995, 7, 1, 14, 27, 13);
        tests.add(new Testdatum(
                    "jurassic.usc.edu - - [01/Aug/1995:14:27:13 -0400] \"GET / \" 200 7",
                    new ExtendedApacheLogRecord("jurassic.usc.edu", null, 
                            null, c.getTime(), "GET", 
                            "/", 
                            null, (short)200, 7, null, null, "19950801-7", ExtendedApacheLogRecord.SessionState.SINGLE)));
        
        c.set(1995, 7, 14, 04, 12, 17);
        tests.add(new Testdatum(
                    "203.16.174.5 - - [14/Aug/1995:04:12:17 -0400] \"GET  HTTP/1.0\" 302 -",
                    new ExtendedApacheLogRecord("203.16.174.5", null, 
                            null, c.getTime(), "GET", 
                            "", 
                            "HTTP/1.0", (short)302, -1, null, null, "19950814-1", ExtendedApacheLogRecord.SessionState.SINGLE)));
        
        
        c.set(1995, 7, 14, 15, 51, 37);
        tests.add(new Testdatum(
                    "128.159.144.83 - - [14/Aug/1995:15:51:37 -0400] \"±‰6žÿT7‰FÃÇF\" 400 -",
                    new ExtendedApacheLogRecord("128.159.144.83", null, 
                            null, c.getTime(), null, 
                            "±‰6žÿT7‰FÃÇF", 
                            null, (short)400, -1, null, null, "19950814-2", ExtendedApacheLogRecord.SessionState.SINGLE)));
    }

    @Test
    public void simple() {
        
        ApacheSessionParser asp = new ApacheSessionParser(1800);
        
        List<ExtendedApacheLogRecord> records = tests.stream().map(t -> ApacheParser.parse(t.input)).map(a -> asp.parse(a)).collect(Collectors.toList());
        
        asp.done();
        
        for (int i = 0; i < records.size(); i++) {
            ExtendedApacheLogRecord expected = tests.get(i).output;
            ExtendedApacheLogRecord actual = records.get(i);
            System.out.println("e: " + expected);
            System.out.println("a: " + actual);            
            assertThat(actual).isEqualTo(expected);
        }
        
    }
    
    @Test
    @Ignore
    public void file() {
        
        try {
            Stream<ApacheLogRecord> parse = ApacheParser.parse(new File("D:\\Downloads\\NASA_access_log_Aug95\\access_log_Aug95"));
            System.out.println("items: " + parse.count());
        } catch (FileNotFoundException ex) {
            fail();
        }
        
    }
    
}
