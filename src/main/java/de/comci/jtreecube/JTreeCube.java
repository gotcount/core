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
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
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

    private final List<String> schema;
    private final JTreeCubeNode root = new JTreeCubeNode();

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
        return count(new ArrayList<>(), groupBy);
    }

    public ResultNode count(List<Filter> filter, String... groupBy) {
        // check that we only aggregate based on the order defined in the schema
        final List<String> groupByList = Arrays.asList(groupBy);
        int[] positions = groupByList.stream().mapToInt(s -> schema.indexOf(s)).toArray();
        if (positions.length > 1) {
            for (int i = 1; i < positions.length; i++) {
                if (positions[i - 1] > positions[i]) {
                    throw new UnsupportedOperationException();
                }
            }
        }
        // aggregate element count
        return root.count(groupByList, filter);
    }

    @Override
    public String toString() {
        return root.toString();
    }
    
    public static class ResultNode {

        public final Object value;
        public AtomicLong count;
        public final Map<Object, ResultNode> children = Collections.synchronizedMap(new HashMap<>());
        private final String key;

        public ResultNode(String key, Object value) {
            this.key = key;
            this.value = value;
            this.count = new AtomicLong(0);
        }
        
        public ResultNode(String key, Object value, long count) {
            this.key = key;
            this.value = value;
            this.count = new AtomicLong(count);
        }

        public ResultNode add(String key, Object value) {
            ResultNode child;
            if (!this.children.containsKey(value)) {
                child = new ResultNode(key, value);
                this.children.put(value, child);
            } else {
                child = this.children.get(value);
            }
            return child;
        }
        
        public ResultNode add(String key, Object value, long count) {
            ResultNode child = add(key, value);
            child.count.addAndGet(count);
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

        public Stream<ResultNode> getChildren(int limit) {
            return getChildren().limit(limit);
        }

        public Stream<ResultNode> getChildren() {
            return children.values().stream().sorted((a, b) -> {
                if (b.count.get() == a.count.get()) {
                    return 0;
                }
                return (b.count.get() > a.count.get()) ? 1 : -1;
            });
        }

        @Override
        public String toString() {
            final StringBuilder str = new StringBuilder();
            toString(str, 0);
            return str.toString();
        }

        public void toString(StringBuilder str, int indent) {
            for (int i = 0; i < indent; i++) {
                str.append("  ");
            }
            str.append(String.format("%s:%s:%d", key, value, count.get()));
            getChildren().forEach(c -> {
                str.append("\n");
                c.toString(str, indent + 1);
            });
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

    public static class Filter {

        private final Predicate<Object> predicate;
        private final String key;

        public Filter(String key, Operation operation, Object... expected) {
            this.key = key;
            final Matcher matcher = getMatcher(operation, expected);
            this.predicate = (Object t) -> matcher.matches(t);
        }

        private Matcher getMatcher(Operation operation, Object... expected) {

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
            return m;
        }

    }

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

        public Set<Object> getValues() {
            return children.keySet();
        }

        private ResultNode count(List<String> groupBy, List<Filter> filter) {
            Map<String, Predicate<Object>> predicates = new HashMap<>();
            filter.forEach(f -> {
                predicates.put(f.key, f.predicate);
            });
            ResultNode node = new ResultNode("", null);
            node.count.set(count(groupBy, node, predicates));
            return node;
        }

        private long count(List<String> groupBy, ResultNode node, final Map<String, Predicate<Object>> predicates) {
            final boolean addNode = groupBy.contains(key);
            long sum = children.entrySet().parallelStream().filter(e -> {
                if (!predicates.containsKey(key)) {
                    return true;
                }
                return predicates.get(key).test(e.getKey());
            }).mapToLong(e -> {
                ResultNode n = node;                
                if (addNode) 
                    n = node.add(key, e.getKey());
                long val = e.getValue().count(groupBy, n, predicates);
                if (addNode)
                    n.count.addAndGet(val);
                return val;
            }).sum();

            if (children.isEmpty()) {
                return count.get();
            } else {
                return sum;
            }

        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            toString(str, 0);            
            return str.toString();
        }
        
        public void toString(StringBuilder str, int indent) {
            for(int i = 0; i < indent; i++) {
                str.append(" ");
            }
            str.append(String.format("%s:%d", key, count.get()));
            if (!children.isEmpty()) {
                children.entrySet().forEach(e -> {
                    str.append("\n");
                    for(int i = 0; i < indent+2; i++) {
                        str.append(" ");
                    }
                    str.append(String.format("->'%s':%d\n", e.getKey().toString(), e.getValue().count.get()));
                    e.getValue().toString(str, indent + 2);
                });
            }
        }
        
    }

    public static class JTreeCubeBuilder {

        private final JTreeCube cube;

        private JTreeCubeBuilder(List<String> schema) {
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

    private static Object[] itemFromRecord(Record record, List<String> schema) {
        return schema.parallelStream().map(key -> {
            return record.getValue(key);
        }).toArray();
    }

}
