/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.cubetree;

import de.comci.cubetree.Dimension;
import java.util.Calendar;
import java.util.Date;
import static org.fest.assertions.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Sebastian
 */
public class DimensionTest {
    
    @Test
    public void intPrecision() {
        
        Dimension d = new Dimension("int", Integer.class).precision(10);
        
        assertThat(d.map(100)).isEqualTo(100);
        assertThat(d.map(101)).isEqualTo(100);
        assertThat(d.map(104)).isEqualTo(100);
        assertThat(d.map(105)).isEqualTo(100);
        assertThat(d.map(109)).isEqualTo(100);
        assertThat(d.map(110)).isEqualTo(110);
        
        d.precision(100);
        assertThat(d.map(0)).isEqualTo(0);
        assertThat(d.map(99)).isEqualTo(0);
        assertThat(d.map(100)).isEqualTo(100);
        assertThat(d.map(101)).isEqualTo(100);
        assertThat(d.map(199)).isEqualTo(100);
        assertThat(d.map(200)).isEqualTo(200);
        assertThat(d.map(201)).isEqualTo(200);
        
        d.precision(-100);
        assertThat(d.map(0)).isEqualTo(0);
        assertThat(d.map(100)).isEqualTo(100);
        
    }
    
    @Test
    public void longPrecision() {
        
        Dimension d = new Dimension("long", Long.class).precision(10);
        
        assertThat(d.map(100l)).isEqualTo(100);
        assertThat(d.map(101l)).isEqualTo(100);
        assertThat(d.map(104l)).isEqualTo(100);
        assertThat(d.map(105l)).isEqualTo(100);
        assertThat(d.map(109l)).isEqualTo(100);
        assertThat(d.map(110l)).isEqualTo(110);
        
        d.precision(100);
        assertThat(d.map(0l)).isEqualTo(0);
        assertThat(d.map(99l)).isEqualTo(0);
        assertThat(d.map(100l)).isEqualTo(100);
        assertThat(d.map(101l)).isEqualTo(100);
        assertThat(d.map(199l)).isEqualTo(100);
        assertThat(d.map(200l)).isEqualTo(200);
        assertThat(d.map(201l)).isEqualTo(200);
        
        d.precision(-100);
        assertThat(d.map(0l)).isEqualTo(0);
        assertThat(d.map(100l)).isEqualTo(100);
        
    }
    
    @Test
    public void intBuckets() {
        
        Dimension d = new Dimension("int", Integer.class).buckets(1000d, 10000d, 100000d, 1000000d);
        
        assertThat(d.map(-5)).isEqualTo(1000);
        assertThat(d.map(0)).isEqualTo(1000);
        assertThat(d.map(1)).isEqualTo(1000);
        assertThat(d.map(999)).isEqualTo(1000);
        assertThat(d.map(1000)).isEqualTo(1000);
        
        assertThat(d.map(1001)).isEqualTo(10000);
        assertThat(d.map(9999)).isEqualTo(10000);
        assertThat(d.map(10000)).isEqualTo(10000);
        
        assertThat(d.map(10001)).isEqualTo(100000);
        assertThat(d.map(99999)).isEqualTo(100000);
        assertThat(d.map(100000)).isEqualTo(100000);
        
        assertThat(d.map(100001)).isEqualTo(1000000);
        assertThat(d.map(999999)).isEqualTo(1000000);
        assertThat(d.map(1000000)).isEqualTo(1000000);
        
        d.buckets(false, 100d,200d,300d,400d,500d);
        assertThat(d.map(100)).isEqualTo(100);
        assertThat(d.map(202)).isEqualTo(200);
        assertThat(d.map(301)).isEqualTo(300);
        assertThat(d.map(404)).isEqualTo(400);
        assertThat(d.map(501)).isEqualTo(500);
        
    }
    
    @Test
    public void dateToYear() {
        
        Dimension d = new Dimension("date", Date.class).precision(Calendar.YEAR);        
        assertThat(d.map(new Date(104,0,1, 12,52,24))).isEqualTo(new Date(104,0,1));
        assertThat(d.map(new Date(100,0,1, 1,24,48 ))).isEqualTo(new Date(100,0,1));
    }
    
    @Test
    public void mapString() {
        
        Dimension d = new Dimension("string", String.class).precision(10);
        
        assertThat(d.map("I am a String of more then 10 characters")).isEqualTo("I am a Str");
        assertThat(d.map("")).isEqualTo("");
        assertThat(d.map((String)null)).isNull();
        assertThat(d.map("0123456789")).isEqualTo("0123456789");
        assertThat(d.map("01234567891")).isEqualTo("0123456789");
        d.precision(-10);
        assertThat(d.map("I am a String of more then 10 characters")).isEqualTo("characters");
        assertThat(d.map("")).isEqualTo("");
        assertThat(d.map((String)null)).isNull();
    }
    
}
