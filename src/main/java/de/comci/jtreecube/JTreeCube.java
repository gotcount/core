/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.jtreecube;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Table;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jooq.Record;
import org.jooq.Result;

/**
 *
 * @author Sebastian
 */
public class JTreeCube {

    private static final Logger LOG = Logger.getLogger(JTreeCube.class.getName());
    private final List<Dimension> schema;
    private final InnerNode root;
    private long lastQueryDuration;

    private JTreeCube(List<Dimension> schema) {
        this.schema = schema.stream().map(s -> new Dimension(s, schema.indexOf(s))).collect(Collectors.toList());
        root = new InnerNode(null, this.schema.get(0), 0);
    }

    private void add(Object[] item) {

        // wrong length
        if (item.length != schema.size()) {
            throw new IllegalArgumentException(String.format("item length (%d) does not match dimension count (%d)", item.length, schema.size()));
        }

        // wrong type
        for (int i = 0; i < schema.size(); i++) {
            final Dimension dim = schema.get(i);
            final Object value = item[i];
            if (value != null && !dim.clasz.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException(String.format("item[%d] '%s' not assignable to %s", i, value.getClass(), dim.clasz));
            }
            // store for histogram
            dim.countValue(value);
        }

        // info
        if (root.count % 25000 == 0) {
            System.out.println(String.format("%d items in tree", root.count));
        }

        // place in tree
        try {
            root.place(schema, item, 0);
        } catch (Exception e) {
            LOG.warning(String.format("could not place %s because: '%s'", Arrays.toString(item), e.getMessage()));
        }

    }

    public long size() {
        return root.count;
    }

    public ResultNode count(String... groupBy) {
        return count(new ArrayList<>(), 10, groupBy);
    }

    public ResultNode count(int topN, String... groupBy) {
        return count(new ArrayList<>(), topN, groupBy);
    }

    public ResultNode count(List<Filter> filter, String... groupBy) {
        return count(filter, 10, groupBy);
    }

    public ResultNode count(List<Filter> filter, int topN, String... groupBy) {

        long time = System.currentTimeMillis();
        try {

            // groupBy to list
            final List<Dimension> groupByList = Arrays.asList(groupBy).stream().map(s -> getDimensionByName(s)).collect(Collectors.toList());

            // if we group only one item without any filters we can do so using 
            // the histogram from the dimension object.
            if (filter.isEmpty() && groupByList.size() == 1) {
                ResultNode result = new ResultNode(null, null);
                final Dimension dimension = groupByList.get(0);
                dimension.getHistogram().entrySet().stream().limit(topN).forEach(e -> {
                    result.add(dimension, e.getKey(), e.getValue());
                    result.count.addAndGet(e.getValue());
                });
                return result;
            }
            
            // check that we only aggregate based on the order defined in the schema
            int[] positions = groupByList.stream().mapToInt(d -> d.depth).toArray();
            if (positions.length > 1) {
                for (int i = 1; i < positions.length; i++) {
                    if (positions[i - 1] > positions[i]) {
                        throw new UnsupportedOperationException();
                    }
                }
            }

            // put filter in map with dimension
            Map<Dimension, Filter> filterMap = filter.stream().collect(Collectors.toMap(f -> getDimensionByName(f.key), f -> f));

            // find max depth we need to visit
            int maxDepth = filter.stream().mapToInt(f -> getDimensionByName(f.key).depth).max().orElse(0);
            maxDepth = Math.max(maxDepth, Arrays.stream(groupBy).mapToInt(d -> getDimensionByName(d).depth).max().orElse(maxDepth));

            // aggregate element count
            ResultNode result = new ResultNode(null, null);            
            result.count.set(root.count(groupByList, filterMap, result, maxDepth));

            // @todo: remove unneccesary elements
            
            return result;

        } finally {
            lastQueryDuration = System.currentTimeMillis() - time;
            LOG.info(String.format("group by %d dimension(s) %s and %d filter(s) %s on %d items performed in %d ms",
                    groupBy.length, Arrays.toString(groupBy),
                    filter.size(), filter.toString(),
                    root.count, lastQueryDuration
            ));
        }

    }

    public long getLastQueryDuration() {
        return lastQueryDuration;
    }

    Dimension getDimensionByName(String name) {
        for (Dimension s : schema) {
            if (s.name.equals(name)) {
                return s;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public String toString() {
        return root.toString();
    }

    private JTreeCube done() {
        // cleanup
        root.cleanup();
        
        System.out.println(String.format("%d items in tree", root.count));
        return this;
    }

    public static class Dimension {

        private final String name;
        private final int depth;
        private final Class<?> clasz;
        private final Multiset<Object> values = HashMultiset.create();
        private int distinctValues = 0;
        
        /**
         * int,long -> (Math.floor(val / precision)) * precision float,double ->
         * (Math.floor(val / precision)) * precision String -> precision > 0 ?
         * val.substr(0,precision) : val.substr(val.length - precision) Date ->
         * 1000*x : sec, 1000*60*x : min, 1000*60*60*x : hour, 1000*60*60*24*x :
         * day * -> precision = 0 ? full
         */
        private final double precision;
        private Map<Object, Integer> histogram;

        private Dimension(Dimension old, int depth) {
            this(old.name, depth, old.clasz, 0);
        }

        public Dimension(String name) {
            this(name, -1, Object.class, 0);
        }

        public Dimension(String name, Class<?> clasz) {
            this(name, -1, clasz, 0);
        }

        public Dimension(String name, Class<?> clasz, double precision) {
            this(name, -1, clasz, precision);
        }

        private Dimension(String name, int depth, Class<?> clasz, double precision) {
            this.name = name;
            this.depth = depth;
            this.clasz = clasz;
            this.precision = precision;
        }

        public synchronized void countValue(Object val) {
            values.add(val);
            histogram = null;
        }

        public Map<Object, Integer> getHistogram() {
            if (histogram == null) {
                // originally I used guava here. But the guava way does not
                // permit null values as keys in the histogram
                histogram = new LinkedHashMap<>();
                values.entrySet().stream().sorted((Multiset.Entry<Object> o1, Multiset.Entry<Object> o2) -> {
                    return o2.getCount() - o1.getCount();
                }).forEach(e -> histogram.put(e.getElement(), e.getCount()));
            }
            return histogram;
        }

        public boolean isComparable() {
            return Comparable.class.isAssignableFrom(this.clasz);
        }

        @Override
        public String toString() {
            return String.format("Dimension:%s:%d:%s", name, depth, clasz.getName());
        }

    }

    public static class ResultNode {

        public Object value;
        public AtomicLong count;
        public final Map<Object, ResultNode> values;
        private final Dimension dimension;
        private final String name;

        private ResultNode(Dimension dimension, Object index) {
            this.dimension = dimension;
            this.name = (dimension != null) ? dimension.name : "";
            //this.value = (dimension != null) ? dimension.getKey(index) : index;
            this.value = index;
            this.count = new AtomicLong(0);
            this.values = new HashMap<>(1000);
        }

        public ResultNode(String name, Object value, long count) {
            this.dimension = null;
            this.name = name;
            this.value = value;
            this.count = new AtomicLong(count);
            this.values = new HashMap<>(1000);
        }

        private ResultNode(Dimension key, Object value, long count) {
            this.dimension = key;
            this.name = key.name;
            this.value = value;
            this.count = new AtomicLong(count);
            this.values = new HashMap<>(1000);
        }

        private synchronized ResultNode add(Dimension key, Object value, long count) {
            ResultNode child;
            if ((child = this.values.get(value)) == null) {
                child = new ResultNode(key, value, count);
                this.values.put(value, child);
            }
            return child;
        }

        /**
         * Only used for testing *hopefully*
         *
         * @param key
         * @param value
         * @param count
         * @return
         */
        public synchronized ResultNode add(String key, Object value, long count) {
            ResultNode child;
            if ((child = this.values.get(value)) == null) {
                child = new ResultNode(key, value, count);
                this.values.put(value, child);
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
            if (obj == this) {
                return true;
            }
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
            if (!this.values.equals(other.values)) {
                return false;
            }
            return true;
        }

        public Stream<ResultNode> getChildren(int limit) {
            return getChildren().limit(limit);
        }

        public Stream<ResultNode> getChildren() {
            return values.values().stream().sorted((a, b) -> {
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
            str.append(String.format("%s:%s:%d", name, value, count.get()));
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
        private final Operation operation;
        private final List values;
  
        public Filter(String key, Operation operation, Object... expected) {
            this.key = key;
            this.predicate = getMatcher(operation, expected);
            this.operation = operation;
            this.values = Arrays.asList(expected);
        }

        private Predicate getMatcher(Operation operation, Object... expected) {

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
            }

            throw new IllegalArgumentException();

        }

        @Override
        public String toString() {
            return String.format("%s is %s %s", key, operation, values);
        }

    }

    private class Node {

        final Node parent;
        final int depth;
        final Dimension dimension;
        long count = 0;
        long cumulativeCount = 0; // count of all values smaller than the current one + the current value (makes only sense if the list is sorted, e.g. not for all dimensions)

        public Node(Node parent, Dimension d, int depth) {
            this.parent = parent;
            this.dimension = d;
            this.depth = depth;
        }

        synchronized void place(List<Dimension> d, Object[] item, int depth) {

            // increment own count
            count++;

            // done when no dimension is left
            if (depth >= item.length) {
                return;
            }

        }

        long count(List<Dimension> groupBy, Map<Dimension, Filter> filters, ResultNode node, int maxDepth) {
            return count;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            toString(str, 0);
            return str.toString();
        }

        synchronized void toString(StringBuilder str, int indent) {
            str.append(count);
        }
        
        void cleanup() {
            // do nothing
        }

    }

    private class InnerNode extends Node {

        private final Map<Object, Node> values;

        private InnerNode(InnerNode parent, Dimension dim, int depth) {
            super(parent, dim, depth);
            if (dim != null && dim.isComparable()) {
                values = new TreeMap<>((o1, o2) -> {
                    if (o1 == null) {
                        return 1;
                    } else if (o2 == null) {
                        return -1;                        
                    }
                    return ((Comparable)o2).compareTo((Comparable)o1);                    
                });
            } else {
                values = new HashMap<>();
            }
        }

        @Override
        synchronized void place(List<Dimension> dimensions, Object[] item, int depth) {

            super.place(dimensions, item, depth);

            // store value
            final Object val = item[depth];
            Node c;
            if ((c = values.get(val)) == null) {
                Dimension dim = (depth + 1 < dimensions.size()) ? dimensions.get(depth + 1) : null;
                if (dim == null) {
                    c = new Node(this, null, depth + 1);
                } else {
                    c = new InnerNode(this, dim, depth + 1);
                }
                values.put(val, c);
            }

            // place next dimension
            c.place(dimensions, item, depth + 1);

        }

        @Override
        long count(List<Dimension> groupBy, Map<Dimension, Filter> filters, ResultNode node, int maxDepth) {

            if (dimension == null || depth > maxDepth || values.isEmpty()) {
                return count;
            }

            final boolean addNode = groupBy.contains(dimension);

            Filter f = filters.get(dimension);
            if (f != null) {
                // needs to be filtered
                Object key = f.key;
                switch (f.operation) {
                    case EQ:
                        Node value = values.get(f.values.get(0));
                        if (value == null) {
                            return 0;
                        }
                        return processNode(addNode, node, groupBy, filters, maxDepth, key, value);
                    case IN:
                        return f.values.parallelStream().mapToLong(v -> {
                            Node val = values.get(v);
                            if (val == null) {
                                return 0;
                            }
                            return processNode(addNode, node, groupBy, filters, maxDepth, key, val);
                        }).sum();
                    default:
                        return values.entrySet().parallelStream().filter(e -> f.predicate.test(e.getKey())).mapToLong(e -> {
                            return processNode(addNode, node, groupBy, filters, maxDepth, e.getKey(), e.getValue());
                        }).sum();
                }
            }

            return values.entrySet().parallelStream().mapToLong(e -> {
                return processNode(addNode, node, groupBy, filters, maxDepth, e.getKey(), e.getValue());
            }).sum();

        }

        private long processNode(final boolean addNode, ResultNode node, List<Dimension> groupBy, Map<Dimension, Filter> filters, int maxDepth, Object key, Node value) {
            if (addNode) {
                ResultNode n;
                synchronized (node) {
                    if ((n = node.values.get(key)) == null) {
                        n = new ResultNode(dimension, key);
                        node.values.put(key, n);
                    }
                }
                long val = value.count(groupBy, filters, n, maxDepth);
                n.count.addAndGet(val);
                return val;
            } else {
                return value.count(groupBy, filters, node, maxDepth);
            }
        }

        @Override
        synchronized void toString(StringBuilder str, int indent) {
            for (int i = 0; i < indent; i++) {
                str.append(" ");
            }
            str.append(String.format("%s:%d", dimension, count));
            if (!values.isEmpty()) {
                values.entrySet().forEach(e -> {
                    str.append("\n");
                    for (int i = 0; i < indent + 2; i++) {
                        str.append(" ");
                    }
                    str.append(String.format("->'%s':%d\n", e.getKey().toString(), e.getValue().count));
                    e.getValue().toString(str, indent + 2);
                });
            }
        }

        @Override
        void cleanup() {
            long cumulativeCount = 0;
            if (dimension.isComparable()) {
                for (Node n : values.values()) {
                    cumulativeCount += n.count;
                    n.cumulativeCount = cumulativeCount;
                    n.cleanup();
                }
            } else {
                values.values().forEach(Node::cleanup);
            }
        }

    }

    public static class JTreeCubeBuilder {

        private final JTreeCube cube;

        private JTreeCubeBuilder(List<Dimension> schema) {
            this.cube = new JTreeCube(schema);
        }

        public JTreeCubeBuilder add(Object... values) {
            cube.add(values);
            return this;
        }

        public JTreeCube done() {
            return cube.done();
        }

    }

    public static JTreeCubeBuilder build(List<String> schema) {
        return new JTreeCubeBuilder(schema.stream().map(s -> new Dimension(s)).collect(Collectors.toList()));
    }

    public static JTreeCubeBuilder build(Dimension... schema) {
        final List<Dimension> asList = Arrays.asList(schema);
        asList.forEach(d -> {
            if (d.clasz.isPrimitive())
                throw new IllegalArgumentException(String.format("primitives like '%s' not allowed", d.clasz));
            if (d == null)
                throw new IllegalArgumentException("dimensions cannot be null");
        });
        return new JTreeCubeBuilder(asList);
    }

    public static JTreeCube build(File file, List<String> schema) throws FileNotFoundException {
        return null;
    }

    public static JTreeCube build(Result<Record> records, List<String> schema) {
        JTreeCube cube = new JTreeCube(schema.stream().map(s -> new Dimension(s)).collect(Collectors.toList()));
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
