/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import org.junit.Test;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Before;

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
        assertThat(t3.keySet(false)).containsExactly("c", "d", "a");
    }

    @Test
    public void bottomThreeHistogram() {
        Histogram<String> t3 = new LimitedHashHistogram<>(-3);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        t3.set("d", 7);
        assertThat(t3.keySet(true)).containsExactly("b", "a", "d");
    }

    @Test
    public void lessThenLimit() {
        Histogram<String> t3 = new LimitedHashHistogram<>(4);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        assertThat(t3.keySet(false)).containsExactly("c", "a", "b");
    }

    @Test
    public void onLimit() {
        Histogram<String> t3 = new LimitedHashHistogram<>(3);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        assertThat(t3.keySet(false)).containsExactly("c", "a", "b");
    }

    @Test
    public void noLimitExplicitConstructor() {
        Histogram<String> t3 = new LimitedHashHistogram<>(0);
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        assertThat(t3.keySet(true)).containsExactly("b", "a", "c");
    }

    @Test
    public void noLimitImplicitConstructor() {
        Histogram<String> t3 = new LimitedHashHistogram<>();
        t3.set("a", 5);
        t3.set("b", 4);
        t3.set("c", 8);
        assertThat(t3.keySet(true)).containsExactly("b", "a", "c");
    }
    
    @Test
    public void toManyElementsWithSameCount() {
        Histogram<String> t3 = new LimitedHashHistogram<>(3);
        t3.set("a", 5);
        t3.set("b", 5);
        t3.set("c", 5);
        t3.set("d", 5);
        t3.set("e", 5);
        assertThat(t3.keySet(false)).containsExactly("a", "b", "c");
    }
    
    @Test
    public void equalsWithHashHistogram() {
        assertThat(h0).isEqualTo(h2);
    }

    @Override
    Histogram<String> getInstance() {
        return new LimitedHashHistogram<>(0);
    }

}
