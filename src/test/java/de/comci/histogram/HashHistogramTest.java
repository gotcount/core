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
public class HashHistogramTest extends HistogramTest {

    @Override
    Histogram<String> getInstance() {
        return new HashHistogram<>();
    }
    
    Histogram<String> h0, h1;
    
    @Before
    public void eachTest() {
        h0 = new HashHistogram<>();
        h0.set("a", 5);
        h0.set("b", 4);
        h1 = new HashHistogram<>();
        h1.set("b", 4);
        h1.set("a", 5);        
    }    
    
    @Test
    public void equalHistograms() {
        assertThat(h0).isEqualTo(h1);
    }
    
    @Test
    public void equalHash() {
        assertThat(h0.hashCode()).isEqualTo(h1.hashCode());
    }
    
}
