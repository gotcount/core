/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.cube;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class Filter {
    
    private final Predicate<Object> predicate;
        private final String key;
        private final CubeTree.Operation operation;
        private final List values;

        public Filter(String key, CubeTree.Operation operation, Object... expected) {
            this.key = key;
            this.predicate = getMatcher(operation, expected);
            this.operation = operation;
            this.values = Arrays.asList(expected);
        }

        public Predicate<Object> getPredicate() {
            return predicate;
        }

        public CubeTree.Operation getOperation() {
            return operation;
        }

        public String getKey() {
            return key;
        }

        public List getValues() {
            return Collections.unmodifiableList(values);
        }
        
        private Predicate getMatcher(CubeTree.Operation operation, Object... expected) {

            double dval;
            Object val = expected[0];
            Set<Object> set;
            switch (operation) {
                case EQ:
                    return (Predicate<Object>) (t) -> ((t != null) && t.equals(val) || t == val);
                case NEQ:
                    return (Predicate<Object>) (t) -> ((t != null) && !t.equals(val) || t != val);
                case GT:
                    dval = ((Number) expected[0]).doubleValue();
                    return (Predicate<Number>) (t) -> t.doubleValue() > dval;
                case GTE:
                    dval = ((Number) expected[0]).doubleValue();
                    return (Predicate<Number>) (t) -> t.doubleValue() >= dval;
                case LT:
                    dval = ((Number) expected[0]).doubleValue();
                    return (Predicate<Number>) (t) -> t.doubleValue() < dval;
                case LTE:
                    dval = ((Number) expected[0]).doubleValue();
                    return (Predicate<Number>) (t) -> t.doubleValue() <= dval;
                case IN:
                    set = new HashSet<>(Arrays.asList(expected));
                    return (Predicate<Object>) (t) -> set.contains(t);
                case NIN:
                    set = new HashSet<>(Arrays.asList(expected));
                    return (Predicate<Object>) (t) -> !set.contains(t);
                case BETWEEN:
                    final Date first = (Date) expected[0];
                    final Date last = (Date) expected[1];
                    return (Predicate<Object>) (t) -> {
                      final Date v = (Date)t;
                      return (first.before(v) || first.equals(v)) && (last.after(v) || last.equals(v));
                    };
            }

            throw new IllegalArgumentException();

        }

        @Override
        public String toString() {
            return String.format("%s is %s %s", key, operation, values);
        }
    
}
