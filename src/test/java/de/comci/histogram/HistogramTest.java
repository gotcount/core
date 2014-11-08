/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import de.comci.bitmap.Value;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.fest.assertions.api.Assertions.assertThat;

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
        assertThat(instance.keySet(true)).containsOnly("a");
        assertThat(instance.get("a")).isEqualTo(1);
    }

    @Test
    public void setExistentToValue() {
        instance.set("a", 1);
        instance.set("a", 2);
        assertThat(instance.keySet(true)).containsOnly("a");
        assertThat(instance.get("a")).isEqualTo(2);
    }

    @Test
    public void setExistentToZero() {
        instance.set("a", 1);
        instance.set("a", 0);
        assertThat(instance.keySet(true)).containsOnly("a");
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
    public void sortedKeySetAsc() {
        instance.set("a", 5);
        instance.set("b", 9);
        instance.set("c", 2);
        assertThat(instance.keySet(true)).containsExactly("c", "a", "b");
    }

    @Test
    public void sortedKeySetDesc() {
        instance.set("a", 5);
        instance.set("b", 9);
        instance.set("c", 2);
        assertThat(instance.keySet(false)).containsExactly("b", "a", "c");
    }

    @Test
    public void emptyKeySet() {
        assertThat(instance.keySet(true)).isEmpty();
    }

    @Test
    public void sortedEntrySetAsc() {
        instance.set("a", 5);
        instance.set("b", 9);
        instance.set("c", 2);
        assertThat(instance.entrySet(true))
                .containsExactly(
                        new HashMap.SimpleEntry<>("c", 2),
                        new HashMap.SimpleEntry<>("a", 5),
                        new HashMap.SimpleEntry<>("b", 9));
    }

    @Test
    public void sortedEntrySetDesc() {
        instance.set("a", 5);
        instance.set("b", 9);
        instance.set("c", 2);
        assertThat(instance.entrySet(false))
                .containsExactly(
                        new HashMap.SimpleEntry<>("b", 9),
                        new HashMap.SimpleEntry<>("a", 5),
                        new HashMap.SimpleEntry<>("c", 2));
    }

    @Test
    public void strangeBehaviour() {

        Multiset<Double> multiset = HashMultiset.create();
        multiset.add(16.0, 105);
        multiset.add(2.0, 197);
        multiset.add(21.0, 97);
        multiset.add(15.0, 107);
        multiset.add(5.0, 204);
        multiset.add(7.0, 183);
        multiset.add(9.0, 107);

        Histogram<Value<Double>> h = new HashHistogram();
        System.out.println("__" + multiset.entrySet().toString());
        System.out.println("__" + multiset.elementSet().toString());
        for (Double t : multiset.elementSet()) {
            System.out.println("__" + t.toString());
            h.set(Value.get(t), multiset.count(t));
        }
        System.out.println("__" + h.toString());
        System.out.println("__" + h.keySet(true).toString());

        assertThat(h.keySet(true)).containsAll(multiset.elementSet().stream().map(d -> Value.get(d)).collect(Collectors.toSet()));

    }

}
