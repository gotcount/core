/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class ValueTest {

    /**
     * Test of getValue method, of class Value.
     */
    @Test
    public void getValue() {
        final String str = "str";
        Value instance = new Value(str, String.class);
        assertThat(instance.getValue()).isSameAs(str);
    }

    /**
     * Test of getType method, of class Value.
     */
    @Test
    public void getType() {
        Value instance = new Value("str", String.class);
        assertThat(instance.getType()).isEqualTo(String.class);
    }

    /**
     * Test of compareTo method, of class Value.
     */
    @Test
    public void compareEqualValuesEqualType() {
        Value i0 = new Value("str", String.class);
        Value i1 = new Value("str", String.class);
        assertThat(i0.compareTo(i1)).isEqualTo(0);
        i0 = new Value(5, Integer.class);
        i1 = new Value(5, Integer.class);
        assertThat(i0.compareTo(i1)).isEqualTo(0);
    }

    @Test
    public void compareDifferentValuesEqualType() {
        Value i0 = new Value("def", String.class);
        Value i1 = new Value("abc", String.class);
        assertThat(i0.compareTo(i1)).isGreaterThan(0);
        i0 = new Value(5, Integer.class);
        i1 = new Value(-10, Integer.class);
        assertThat(i0.compareTo(i1)).isGreaterThan(0);
    }

    @Test
    public void divertingTypes() {
        try {
            Value v = new Value("str", Integer.class);
            fail("missing exception");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("type is not assignable (class java.lang.String != class java.lang.Integer)");
        }
    }

    @Test
    public void noTypeGiven() {
        try {
            Value v = new Value("str", null);
            fail("missing exception");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("type must not be null");
        }
    }

    @Test
    public void compareEqualValuesOtherType() {
        Value i0 = new Value(1, Integer.class);
        Value i1 = new Value(1l, Long.class);
        try {
            i0.compareTo(i1);
            fail("missing exception");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("cannot compare values of different type");
        }
    }

    @Test
    public void sameHashCode() {
        Value i0 = new Value("str", String.class);
        Value i1 = new Value("str", String.class);
        assertThat(i0.hashCode()).isEqualTo(i1.hashCode());
    }

    @Test
    public void differentHashCode() {
        Value i0 = new Value("str", String.class);
        Value i1 = new Value("Str", String.class);
        assertThat(i0.hashCode()).isNotEqualTo(i1.hashCode());
    }

}
