/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.jtreecube;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SortOrder;

/**
 *
 * @author Sebastian
 */
public class JTreeCube {

    public static JTreeCube build(File file, List<String> schema) throws FileNotFoundException {
        return null;
    }

    public static JTreeCube build(Result<Record> records, List<String> schema) {
        JTreeCube cube = new JTreeCube(schema);
        records.forEach(record -> {
            cube.add(itemFromRecord(record, schema));
        });
        return cube;
    }

    public static class JTreeCubeBuilder {

        private final JTreeCube cube;

        public JTreeCubeBuilder(List<String> schema) {
            this.cube = new JTreeCube(schema);
        }

        public JTreeCubeBuilder add(Object... values) {
            cube.add(values);
            return this;
        }

        public JTreeCube done() {
            return cube;
        }

    }

    public static JTreeCubeBuilder build(List<String> schema) {
        return new JTreeCubeBuilder(schema);
    }

    private static Object[] itemFromRecord(Record record, List<String> schema) {
        return schema.parallelStream().map(key -> {
            return record.getValue(key);
        }).toArray();
    }
    private final List<String> schema;
    private final JTreeCubeNode root = new JTreeCubeNode();

    private class JTreeCubeNode {

        private final JTreeCubeNode parent;
        private String key;
        private final AtomicLong count = new AtomicLong();
        private final Map<Object, JTreeCubeNode> children = Collections.synchronizedMap(new HashMap<>());
        private final int depth;

        private JTreeCubeNode() {
            this.key = null;
            this.parent = null;
            this.depth = 0;
        }

        public JTreeCubeNode(JTreeCubeNode parent, int depth) {
            this.parent = parent;
            this.depth = depth;
        }

        private void place(List<String> schema, Object[] item, int depth) {
            count.incrementAndGet();
            if (item.length == depth) {
                return;
            }
            final Object val = item[depth];
            this.key = schema.get(depth);
            JTreeCubeNode c;
            if (!children.containsKey(val)) {
                children.put(val, (c = new JTreeCubeNode(this, depth + 1)));
            } else {
                c = children.get(val);
            }
            c.place(schema, item, depth + 1);
        }

        private void aggregate(String field, List<Filter> filter, ArrayList<Pair<String, Long>> list) {
            Filter filterThisLevel = null;
            try {
                // find relevant filter
                filterThisLevel = filter.stream().filter(p -> p.key == this.key).findFirst().get();
                // remove from filter set
                filter.remove(filterThisLevel);
            } catch (NoSuchElementException ex) {
                //
            }

            // check filter applies
        }

        private ResultNode count(List<String> groupBy) {
            return count(groupBy, new ResultNode("", null, count.get()));
        }

        private ResultNode count(List<String> groupBy, ResultNode node) {
            if (!groupBy.isEmpty()) {
                final List<String> reduced = new ArrayList<>(groupBy);
                final boolean addNode = reduced.contains(key);
                if (addNode) {
                    reduced.remove(key);
                }
                children.entrySet().forEach(e -> {
                    ResultNode n = node;
                    if (addNode) 
                        n = node.add(key, e.getKey(), e.getValue().count.get());
                    e.getValue().count(reduced, n);
                });
            }
            return node;
        }

    }

    private JTreeCube(List<String> schema) {
        this.schema = schema;
    }

    private void add(Object[] item) {
        if (item.length != schema.size()) {
            throw new IllegalArgumentException();
        }
        if (root.count.get() % 25000 == 0) {
            System.out.println(String.format("%d items in tree", root.count.get()));
        }
        root.place(schema, item, 0);
    }

    public long size() {
        return root.count.get();
    }

    public ResultNode count(String... groupBy) {
        return root.count(Arrays.asList(groupBy));
    }

    public Stream<Pair<String, Long>> aggregate(String field, SortOrder sortOrder, List<Filter> filter) {
        final ArrayList<Pair<String, Long>> list = new ArrayList<Pair<String, Long>>(100);
        this.root.aggregate(field, filter, list);
        return list.parallelStream().sorted(new Comparator<Pair<String, Long>>() {

            @Override
            public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                return (sortOrder == SortOrder.ASC) ? (int) (o1.value - o2.value) : (int) (o2.value - o1.value);
            }

        });
    }

    public static class ResultNode {

        public final Object value;
        public AtomicLong count;
        public final Map<Object, ResultNode> children = Collections.synchronizedMap(new HashMap<>());
        private final String key;

        public ResultNode(String key, Object value, long count) {
            this.key = key;
            this.value = value;
            this.count = new AtomicLong(count);
        }

        public ResultNode add(String key, Object value, long count) {
            ResultNode child;
            if (!this.children.containsKey(value)) {
                child = new ResultNode(key, value, count);
                this.children.put(value, child);
            } else {
                child = this.children.get(value);
                child.count.addAndGet(count);
            }
            return child;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ResultNode other = (ResultNode) obj;
            if (!Objects.equals(this.value, other.value)) {
                return false;
            }
            if (this.count.get() != other.count.get()) {
                return false;
            }
            if (!this.children.equals(other.children)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return String.format("%s:%s:%d -> %s", key, value, count.get(), children.values().stream().map(c -> "\n" + c.toString()).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString());
        }

    }

    public enum Operation {

        EQ,
        NEQ,
        IN,
        NIN,
        GT,
        GTE,
        LT,
        LTE;

    }

    public static class Pair<K, V> {

        K key;
        V value;
    }

    public static class Filter {

        String key;
        Object[] expected;
        Operation operation;

        public boolean applies(Object value) {

            Matcher m = null;
            switch (operation) {
                case EQ:
                    m = Matchers.equalTo(expected[0]);
                    break;
                case NEQ:
                    m = Matchers.not(Matchers.equalTo(expected[0]));
                    break;
                case GT:
                    m = Matchers.greaterThan((Double) expected[0]);
                    break;
                case GTE:
                    m = Matchers.greaterThanOrEqualTo((Double) expected[0]);
                    break;
                case LT:
                    m = Matchers.lessThan((Double) expected[0]);
                    break;
                case LTE:
                    m = Matchers.lessThanOrEqualTo((Double) expected[0]);
                    break;
                case IN:
                    m = Matchers.isIn(expected);
                    break;
                case NIN:
                    m = Matchers.not(Matchers.isIn(expected));
                    break;
            }
            return m.matches(value);
        }

    }

}
