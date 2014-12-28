/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class LimitedHashHistogramTest extends HashHistogramTest {
    
    Histogram h2;
        
    @Before
    @Override
    public void eachTest() {
        h0 = new LimitedHashHistogram<>(0);
        h0.set("a", 5);
        h0.set("b", 4);
        h1 = new LimitedHashHistogram<>(0);
        h1.set("b", 4);
        h1.set("a", 5);        
        h2 = new HashHistogram();
        h2.set("b", 4);
        h2.set("a", 5);
    }    

    @Test
    public void topThreeHistogram() {
        Histogram<String> t3 = new LimitedHashHistogram<>(3);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        t3.set("d", 7);
        assertThat(t3.keySet()).containsOnly("c", "d", "a");
    }

    @Test
    public void bottomThreeHistogram() {
        Histogram<String> t3 = new LimitedHashHistogram<>(-3);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        t3.set("d", 7);
        assertThat(t3.keySet()).containsOnly("b", "a", "d");
    }

    @Test
    public void lessThenLimit() {
        Histogram<String> t3 = new LimitedHashHistogram<>(4);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        assertThat(t3.keySet()).containsOnly("c", "a", "b");
    }

    @Test
    public void onLimit() {
        Histogram<String> t3 = new LimitedHashHistogram<>(3);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        assertThat(t3.keySet()).containsOnly("c", "a", "b");
    }

    @Test
    public void noLimitExplicitConstructor() {
        Histogram<String> t3 = new LimitedHashHistogram<>(0);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        assertThat(t3.keySet()).containsOnly("b", "a", "c");
    }

    @Test
    public void noLimitImplicitConstructor() {
        Histogram<String> t3 = new LimitedHashHistogram<>();
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        assertThat(t3.keySet()).containsOnly("b", "a", "c");
    }
    
    @Test
    public void toManyElementsWithSameCount() {
        Histogram<String> t3 = new LimitedHashHistogram<>(3);
        t3.set("a", 5);
        t3.set("b", 5);
        t3.set("c", 5);
        t3.set("d", 5);
        t3.set("e", 5);
        assertThat(t3.keySet()).containsOnly("a", "b", "c");
    }
    
    @Test
    public void equalsWithHashHistogram() {
        assertThat(h0).isEqualTo(h2);
    }
    
    @Test
    public void allTheSameValue() {
        
        LimitedHashHistogram<String> l = new LimitedHashHistogram<>(2);
        
        l.set("a", 5);
        l.set("b", 5);
        l.set("c", 5);
        l.set("d", 5);
        l.set("e", 5);
        l.set("f", 5);
        
        HashHistogram<String> expected = new HashHistogram<>();
        expected.set("a", 5);
        expected.set("b", 5);
        
        assertThat(l).isEqualTo(expected);
        
    }
    
    @Test
    public void increasingValue() {
        
        LimitedHashHistogram<String> l = new LimitedHashHistogram<>(2);
        
        l.set("a", 1);
        l.set("b", 2);
        l.set("c", 3);
        l.set("d", 4);
        l.set("e", 5);
        l.set("f", 6);
        l.set("g", 7);
        l.set("h", 8);
        l.set("i", 9);
        l.set("j", 10);
        l.set("k", 11);
        l.set("l", 12);
        
        HashHistogram<String> expected = new HashHistogram<>();
        expected.set("l", 12);
        expected.set("k", 11);
        
        assertThat(l).isEqualTo(expected);
        
    }
    
    @Test
    public void parabel() {
        
        LimitedHashHistogram<String> l = new LimitedHashHistogram<>(2);
        
        l.set("a", 1);
        l.set("b", 2);
        l.set("c", 3);
        l.set("d", 4);
        l.set("e", 5);
        l.set("f", 6);
        l.set("g", 6);
        l.set("h", 5);
        l.set("i", 4);
        l.set("j", 3);
        l.set("k", 2);
        l.set("l", 1);
        
        HashHistogram<String> expected = new HashHistogram<>();
        expected.set("f", 6);
        expected.set("g", 6);
        
        assertThat(l).isEqualTo(expected);
        
    }
    
    @Test
    public void alternating() {
        
        LimitedHashHistogram<String> l = new LimitedHashHistogram<>(2);
        
        l.set("a", 1);
        l.set("b", 5);
        l.set("c", 3);
        l.set("d", 11);
        l.set("e", 7);
        l.set("f", 21);
        l.set("g", 13);
        l.set("h", 29);
        l.set("i", 17);
        l.set("j", 37);
        l.set("k", 21);
        l.set("l", 45);
        
        HashHistogram<String> expected = new HashHistogram<>();
        expected.set("l", 45);
        expected.set("j", 37);
        
        assertThat(l).isEqualTo(expected);
        
    }
    
    @Test
    public void lotsOfZeros() {
        LimitedHashHistogram<String> l = new LimitedHashHistogram<>(2);
        
        l.set("a", 1);
        l.set("b", 0);
        l.set("c", 0);
        l.set("d", 0);
        l.set("e", 5);
        l.set("f", 0);
        l.set("g", 0);
        l.set("h", 0);
        l.set("i", 9);
        l.set("j", 3);
        l.set("k", 0);
        l.set("l", 0);
        
        HashHistogram<String> expected = new HashHistogram<>();
        expected.set("i", 9);
        expected.set("e", 5);
        
        assertThat(l).isEqualTo(expected);
    }
    
    @Test
    public void setExistingKey() {
        
        LimitedHashHistogram<String> l = new LimitedHashHistogram<>(2);
        
        l.set("a", 1);
        l.set("b", 2);
        l.set("c", 3);
        l.set("b", 4);
        
        HashHistogram<String> expected = new HashHistogram<>();
        expected.set("b", 4);
        expected.set("c", 3);
        
        assertThat(l).isEqualTo(expected);
        
    }

    @Override
    Histogram<String> getInstance() {
        return new LimitedHashHistogram<>(0);
    }

}
