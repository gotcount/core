/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.NoSuchElementException;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public abstract class HistogramTest {

    private Histogram<String> instance;

    @Before
    public void beforeEach() {
        instance = getInstance();
    }

    abstract Histogram<String> getInstance();

    @Test(expected = NoSuchElementException.class)
    public void getNonExistent() {
        Integer get = instance.get("a");
    }

    @Test
    public void getSome() {
        instance.set("a", 5);
        assertThat(instance.get("a")).isEqualTo(5);
    }

    @Test
    public void setNonExistentToValue() {
        instance.set("a", 1);
        assertThat(instance.keySet()).containsOnly("a");
        assertThat(instance.get("a")).isEqualTo(1);
    }

    @Test
    public void setExistentToValue() {
        instance.set("a", 1);
        instance.set("a", 2);
        assertThat(instance.keySet()).containsOnly("a");
        assertThat(instance.get("a")).isEqualTo(2);
    }

    @Test
    public void setExistentToZero() {
        instance.set("a", 1);
        instance.set("a", 0);
        assertThat(instance.keySet()).containsOnly("a");
        assertThat(instance.get("a")).isEqualTo(0);
    }

    @Test
    public void emptySize() {
        assertThat(instance.size()).isEqualTo(0);
    }

    @Test
    public void nonEmptySize() {
        instance.set("a", 5);
        assertThat(instance.size()).isEqualTo(1);
    }

    @Test
    public void removeExistent() {
        instance.set("a", 5);
        instance.set("b", 1);
        instance.remove("a");
        assertThat(instance.size()).isEqualTo(1);
    }

    @Test
    public void removeNonExistent() {
        try {
            instance.remove("a");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void hasElementTrue() {
        instance.set("a", 1);
        assertThat(instance.has("a")).isTrue();
    }

    @Test
    public void hasElementFalse() {
        assertThat(instance.has("a")).isFalse();
    }

    @Test
    public void clear() {
        instance.set("a", 5);
        instance.clear();
        assertThat(instance.size()).isEqualTo(0);
    }

    @Test
    public void clearEmpty() {
        instance.clear();
        assertThat(instance.size()).isEqualTo(0);
    }

    @Test
    public void keySet() {
        instance.set("a", 5);
        instance.set("b", 9);
        instance.set("c", 2);
        assertThat(instance.keySet()).containsOnly("c", "a", "b");
    }
    
    @Test
    public void emptyKeySet() {
        assertThat(instance.keySet()).isEmpty();
    }
    
    @Test
    public void remove() {
        instance.set("a", 4);
        instance.set("b", 6);
        instance.set("c", 4);
        assertThat(instance.remove("a")).isEqualTo(4);
        assertThat(instance.keySet()).containsOnly("c", "b");
    }

    @Test
    public void iterator() {
        instance.set("a", 5);
        instance.set("b", 9);
        instance.set("c", 2);
        assertThat(instance)
                .containsOnly(
                        new HashHistogram.SimpleEntry<>("c", 2),
                        new HashHistogram.SimpleEntry<>("a", 5),
                        new HashHistogram.SimpleEntry<>("b", 9));
    }

    @Test
    public void sameCountForDifferentItemsCanActuallyHappen() {

        Multiset<String> multiset = HashMultiset.create();
        multiset.add("a", 105);
        multiset.add("b", 197);
        multiset.add("c", 97);
        multiset.add("d", 107);
        multiset.add("e", 204);
        multiset.add("f", 183);
        multiset.add("g", 107);

        Histogram<String> h = getInstance();
        for (String t : multiset.elementSet()) {
            h.set(t, multiset.count(t));
        }

        assertThat(h.keySet()).containsAll(multiset.elementSet());

    }

}
