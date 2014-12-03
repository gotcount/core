/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.google.common.collect.HashBiMap;
import de.comci.histogram.HashHistogram;
import de.comci.histogram.Histogram;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import static org.fest.assertions.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class Bugfixing {

    private Date d(LocalDate ld) {
        return Date.from(ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }
    
    @Test
    public void filterSmallDateColumn() {

        BitMapCollection instance = BitMapCollection.create()
                .dimension("d0", Date.class)
                .build();

        instance.add(new Object[]{d(LocalDate.now().minusDays(4))});
        instance.add(new Object[]{d(LocalDate.now().minusDays(3))});
        instance.add(new Object[]{d(LocalDate.now().minusDays(2))});
        instance.add(new Object[]{d(LocalDate.now().minusDays(2))});
        instance.add(new Object[]{d(LocalDate.now().minusDays(1))});
        instance.add(new Object[]{d(LocalDate.now().minusDays(1))});
        instance.add(new Object[]{d(LocalDate.now().minusDays(2))});
        instance.add(new Object[]{d(LocalDate.now().minusDays(2))});
        instance.add(new Object[]{d(LocalDate.now().minusDays(0))});

        instance.build();

        Histogram expected = new HashHistogram();
        expected.set(Value.get(d(LocalDate.now().minusDays(3))), 1);
        expected.set(Value.get(d(LocalDate.now().minusDays(1))), 2);

        Map<String, Predicate> filters = new HashMap<>();
        filters.put("d0", p -> Arrays.asList(d(LocalDate.now().minusDays(3)), d(LocalDate.now().minusDays(1))).contains(p));

        assertThat(instance.histogram("d0", filters)).isEqualTo(expected);
    }

    @Test
    public void filterDateColumn() {

        BitMapCollection instance = BitMapCollection.create()
                .dimension("d0", LocalDate.class)
                .build();

        int size = 1000;
        for (int i = 0; i < size; i++) {
            int d = i;
            if (i % 11 == 0) {
                d = 5;
            }
            if (i % 7 == 0) {
                d = 2;
            }
            if (i % 37 == 0) {
                d = 17;
            }
            instance.add(new Object[]{LocalDate.now().minusDays(d)});
        }

        instance.build();

        Histogram expected = new HashHistogram();
        expected.set(Value.get(LocalDate.now().minusDays(5)), 77);
        expected.set(Value.get(LocalDate.now().minusDays(2)), 140);
        expected.set(Value.get(LocalDate.now().minusDays(17)), 29);

        Map<String, Predicate> filters = new HashMap<>();
        filters.put("d0", p -> Arrays.asList(LocalDate.now().minusDays(5), LocalDate.now().minusDays(2), LocalDate.now().minusDays(17)).contains(p));

        assertThat(instance.histogram("d0", filters)).isEqualTo(expected);

    }

}
