/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.cube;

import de.comci.cube.TimeUnit;
import java.util.Date;
import static org.fest.assertions.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Sebastian
 */
public class DimensionTimeUnitTest {
    
    public DimensionTimeUnitTest() {
    }

    @Test
    public void testConvertYear() {
        Date actualDate = new Date(102, 0, 5);
        int expected = 2002;
        TimeUnit instance = TimeUnit.YEAR;        
        int actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void testConvertQuarter() {
        Date actualDate = new Date(102, 0, 5);
        int expected = 1;
        TimeUnit instance = TimeUnit.QUARTER;        
        int actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 3, 5);
        expected = 2;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 5, 5);
        expected = 2;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 6, 1);
        expected = 3;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 9, 30);
        expected = 4;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 11, 31);
        expected = 4;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void testConvertMonth() {
        Date actualDate = new Date(102, 0, 5);
        int expected = 0;
        TimeUnit instance = TimeUnit.MONTH;        
        int actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void testConvertWeek() {
        Date actualDate = new Date(102, 0, 5);
        int expected = 1;
        TimeUnit instance = TimeUnit.WEEK;        
        int actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 10, 17);
        expected = 46;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 10, 18);
        expected = 47;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 10, 24);
        expected = 47;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 10, 25);
        expected = 48;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(102, 11, 30);
        expected = 1;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
        
        actualDate = new Date(104, 11, 27);
        expected = 53;
        actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void testConvertDay() {
        Date actualDate = new Date(102, 0, 5);
        int expected = 5;
        TimeUnit instance = TimeUnit.DAY;        
        int actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void testConvertHour() {
        Date actualDate = new Date(102, 0, 5, 13, 58, 51);
        int expected = 13;
        TimeUnit instance = TimeUnit.HOUR;        
        int actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void testConvertMinute() {
        Date actualDate = new Date(102, 0, 5, 13, 58, 51);
        int expected = 58;
        TimeUnit instance = TimeUnit.MINUTE;        
        int actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    public void testConvertSecond() {
        Date actualDate = new Date(102, 0, 5, 13, 58, 51);
        int expected = 51;
        TimeUnit instance = TimeUnit.SECOND;        
        int actual = instance.get(actualDate);
        assertThat(actual).isEqualTo(expected);
    }
    
}
