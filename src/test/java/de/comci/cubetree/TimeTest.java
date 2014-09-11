/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.cubetree;

import de.comci.cubetree.TimeUnit;
import de.comci.cubetree.Time;
import java.util.Date;
import static org.fest.assertions.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Sebastian
 */
public class TimeTest {

    public TimeTest() {
    }

    /**
     * Test of compareTo method, of class Time.
     */
    @Test
    public void compareEqualPrecision() {
        Time t0 = new Time(new Date(104, 2, 25), TimeUnit.YEAR);
        Time t1 = new Time(new Date(105, 2, 25), TimeUnit.YEAR);

        assertThat(t0.compareTo(t1)).isEqualTo(-1);
        assertThat(t1.compareTo(t0)).isEqualTo(1);

        t1 = new Time(new Date(104, 2, 26), TimeUnit.YEAR);
        assertThat(t0.compareTo(t1)).isEqualTo(0);

        t1 = new Time(new Date(104, 2, 24), TimeUnit.YEAR);
        assertThat(t0.compareTo(t1)).isEqualTo(0);

        t1 = new Time(new Date(103, 11, 31), TimeUnit.YEAR);
        assertThat(t0.compareTo(t1)).isEqualTo(1);
    }

    @Test
    public void compareYearAndQuarter() {

        Time t0 = new Time(new Date(104, 4, 25), TimeUnit.YEAR);
        Time t1 = new Time(new Date(105, 2, 25), TimeUnit.QUARTER);

        assertThat(t0.compareTo(t1)).isEqualTo(-1);
        assertThat(t1.compareTo(t0)).isEqualTo(1);

        t1 = new Time(new Date(104, 4, 26), TimeUnit.QUARTER);
        assertThat(t0.compareTo(t1)).isEqualTo(0);

        t1 = new Time(new Date(104, 3, 25), TimeUnit.QUARTER);
        assertThat(t0.compareTo(t1)).isEqualTo(0);

        t1 = new Time(new Date(104, 5, 25), TimeUnit.QUARTER);
        assertThat(t0.compareTo(t1)).isEqualTo(0);

        t1 = new Time(new Date(104, 0, 25), TimeUnit.QUARTER);
        assertThat(t0.compareTo(t1)).isEqualTo(1);

        t1 = new Time(new Date(104, 6, 25), TimeUnit.QUARTER);
        assertThat(t0.compareTo(t1)).isEqualTo(-1);

    }
    
    @Test
    public void compareMonth() {
        Time t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.MONTH);
        Time t1 = new Time(new Date(104, 4, 2, 0, 58, 18), TimeUnit.MONTH);
        
        assertThat(t0.compareTo(t1)).isEqualTo(0);
        
        t1 = new Time(new Date(104, 4, 1, 0, 0, 0), TimeUnit.MONTH);
        assertThat(t0.compareTo(t1)).isEqualTo(0);
        
        t1 = new Time(new Date(104, 4, 31, 23, 59, 59), TimeUnit.MONTH);
        assertThat(t0.compareTo(t1)).isEqualTo(0);
        
        t1 = new Time(new Date(104, 3, 30, 23, 59, 59), TimeUnit.MONTH);        
        assertThat(t0.compareTo(t1)).isEqualTo(1);
    }

    @Test
    public void compareWeekAndHour() {

        Time t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.WEEK);
        Time t1 = new Time(new Date(105, 2, 25, 0, 58, 18), TimeUnit.HOUR);
        
        assertThat(t0.compareTo(t1)).isEqualTo(-1);
        assertThat(t1.compareTo(t0)).isEqualTo(1);

        t1 = new Time(new Date(103, 2, 25, 8, 58, 18), TimeUnit.HOUR);
        assertThat(t0.compareTo(t1)).isEqualTo(1);
        
        t1 = new Time(new Date(104, 4, 25, 4, 5, 26), TimeUnit.HOUR);
        assertThat(t0.compareTo(t1)).isEqualTo(0);
        
    }
    
    @Test
    public void testHash() {
        
        Time t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.MONTH);
        Time t1 = new Time(new Date(104, 4, 2, 0, 58, 18), TimeUnit.MONTH);
        
        assertThat(t0.hashCode()).isEqualTo(t1.hashCode());
        
        t1 = new Time(new Date(104, 4, 1, 0, 0, 0), TimeUnit.MONTH);
        assertThat(t0.hashCode()).isEqualTo(t1.hashCode());
        
        t1 = new Time(new Date(104, 4, 31, 23, 59, 59), TimeUnit.MONTH);
        assertThat(t0.hashCode()).isEqualTo(t1.hashCode());
        
        t1 = new Time(new Date(104, 3, 30, 23, 59, 59), TimeUnit.MONTH);
        assertThat(t0.hashCode()).isNotEqualTo(t1.hashCode());
        
        t1 = new Time(new Date(104, 5, 1, 0, 0, 0), TimeUnit.MONTH);
        assertThat(t0.hashCode()).isNotEqualTo(t1.hashCode());
        
        t1 = new Time(new Date(104, 5, 1, 0, 0, 0), TimeUnit.YEAR);
        assertThat(t0.hashCode()).isNotEqualTo(t1.hashCode());
        
        t1 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.YEAR);
        assertThat(t0.hashCode()).isNotEqualTo(t1.hashCode());
        
    }
    
    @Test
    public void testEquals() {
        
        Time t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.MONTH);
        Time t1 = new Time(new Date(104, 4, 2, 0, 58, 18), TimeUnit.MONTH);
        
        assertThat(t0.equals(t1)).isTrue();
        assertThat(t0.equals(null)).isFalse();
        assertThat(t0.equals(5)).isFalse();
        
        t1 = new Time(new Date(104, 3, 2, 0, 58, 18), TimeUnit.YEAR);
        assertThat(t0.equals(t1)).isFalse();
        
        t1 = new Time(new Date(104, 4, 2, 0, 58, 18), TimeUnit.DAY);
        assertThat(t0.equals(t1)).isFalse();
        
        t1 = new Time(new Date(104, 4, 25, 0, 58, 18), TimeUnit.DAY);
        assertThat(t0.equals(t1)).isTrue();
        
        t1 = new Time(new Date(104, 4, 28, 18, 58, 18), TimeUnit.MONTH);
        assertThat(t0.equals(t1)).isTrue();
        
    }
    
    @Test
    public void testToString() {
        
        Time t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.YEAR);
        assertThat(t0.toString()).isEqualTo("2004");
        
        t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.QUARTER);
        assertThat(t0.toString()).isEqualTo("2004-Q2");
        
        t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.MONTH);
        assertThat(t0.toString()).isEqualTo("2004-05");
        
        t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.WEEK);
        assertThat(t0.toString()).isEqualTo("2004-W22");
        
        t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.DAY);
        assertThat(t0.toString()).isEqualTo("2004-05-25");
        
        t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.HOUR);
        assertThat(t0.toString()).isEqualTo("2004-05-25:04");
        
        t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.MINUTE);
        assertThat(t0.toString()).isEqualTo("2004-05-25:04:05");
        
        t0 = new Time(new Date(104, 4, 25, 4, 5, 25), TimeUnit.SECOND);
        assertThat(t0.toString()).isEqualTo("2004-05-25:04:05:25");
        
    }

}
