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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.nashorn.internal.runtime.arrays.ArrayLikeIterator;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Sebastian
 */
public class ApacheParserTest {
    
    public ApacheParserTest() {
    }
   
    private static class Testdatum {        
        String input;
        ApacheLogRecord output; 

        public Testdatum(String input, ApacheLogRecord output) {
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
                    new ApacheLogRecord("in24.inetnebr.com", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/missions/sts-68/news/sts-68-mcc-05.txt", 
                            "HTTP/1.0", (short)200, 1839)));
        
        c.set(1995, 7, 1, 0, 0, 51);
        tests.add(new Testdatum(
                    "133.43.96.45 - - [01/Aug/1995:00:00:51 -0400] \"GET /shuttle/resources/orbiters/orbiters-logo.gif HTTP/1.0\" 200 1932",
                    new ApacheLogRecord("133.43.96.45", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/resources/orbiters/orbiters-logo.gif", 
                            "HTTP/1.0", (short)200, 1932)));
        
        c.set(1995, 7, 1, 0, 2, 13);
        tests.add(new Testdatum(
                    "piweba1y.prodigy.com - - [01/Aug/1995:00:02:13 -0400] \"GET / HTTP/1.0\" 200 7280",
                    new ApacheLogRecord("piweba1y.prodigy.com", null, 
                            null, c.getTime(), "GET", 
                            "/", 
                            "HTTP/1.0", (short)200, 7280)));
        
        c.set(1995, 7, 1, 0, 3, 53);
        tests.add(new Testdatum(
                    "gw1.att.com - - [01/Aug/1995:00:03:53 -0400] \"GET /shuttle/missions/sts-73/news HTTP/1.0\" 302 -",
                    new ApacheLogRecord("gw1.att.com", null, 
                            null, c.getTime(), "GET", 
                            "/shuttle/missions/sts-73/news", 
                            "HTTP/1.0", (short)302, -1)));
        
        c.set(1995, 7, 1, 0, 12, 37);
        tests.add(new Testdatum(
                    "pipe1.nyc.pipeline.com - - [01/Aug/1995:00:12:37 -0400] \"GET /history/apollo/apollo-13/apollo-13-patch-small.gif\" 200 12859",
                    new ApacheLogRecord("pipe1.nyc.pipeline.com", null, 
                            null, c.getTime(), "GET", 
                            "/history/apollo/apollo-13/apollo-13-patch-small.gif", 
                            null, (short)200, 12859)));
        
        c.set(1995, 7, 1, 14, 27, 13);
        tests.add(new Testdatum(
                    "jurassic.usc.edu - - [01/Aug/1995:14:27:13 -0400] \"GET / \" 200 7",
                    new ApacheLogRecord("jurassic.usc.edu", null, 
                            null, c.getTime(), "GET", 
                            "/", 
                            null, (short)200, 7)));
        
        c.set(1995, 7, 14, 04, 12, 17);
        tests.add(new Testdatum(
                    "203.16.174.5 - - [14/Aug/1995:04:12:17 -0400] \"GET  HTTP/1.0\" 302 -",
                    new ApacheLogRecord("203.16.174.5", null, 
                            null, c.getTime(), "GET", 
                            "", 
                            "HTTP/1.0", (short)302, -1)));
        
        
        c.set(1995, 7, 14, 15, 51, 37);
        tests.add(new Testdatum(
                    "128.159.144.83 - - [14/Aug/1995:15:51:37 -0400] \"±‰6žÿT7‰FÃÇF\" 400 -",
                    new ApacheLogRecord("128.159.144.83", null, 
                            null, c.getTime(), null, 
                            "±‰6žÿT7‰FÃÇF", 
                            null, (short)400, -1)));
    }

    @Test
    public void simple() {
        tests.forEach(t -> { 
            ApacheLogRecord actual = ApacheParser.parse(t.input);
            System.out.println("e: " + t.output);
            System.out.println("a: " + actual);            
            assertThat(actual).isEqualTo(t.output);
        });
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
