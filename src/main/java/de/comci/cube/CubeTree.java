/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.cube;

import com.google.common.cache.Cache;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jooq.Record;
import org.jooq.Result;

/**
 *
 * @author Sebastian
 */
public class CubeTree {

    private static final Logger LOG = Logger.getLogger(CubeTree.class.getName());
    private final List<CubeDimension> schema;
    private final InnerNode root;
    private long lastQueryDuration;

    private CubeTree(List<Dimension> schema) {
        this.schema = schema.stream().map(s -> new CubeDimension(s, schema.indexOf(s))).collect(Collectors.toList());
        root = new InnerNode(null, this.schema.get(0), 0);
    }

    private void add(Object[] item) {

        // wrong length
        if (item.length != schema.size()) {
            throw new IllegalArgumentException(String.format("item length (%d) does not match dimension count (%d)", item.length, schema.size()));
        }

        // wrong type
        for (int i = 0; i < schema.size(); i++) {
            final CubeDimension dim = schema.get(i);
            final Object value = item[i];
            if (value != null && !dim.clasz.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException(String.format("item[%d] '%s' not assignable to %s", i, value.getClass(), dim.clasz));
            }
            // get value in set precision
            Object precision = dim.map(value);
            item[i] = precision;
            // store for histogram
            dim.countValue(precision);
        }

        // info
        if (root.count % 50000 == 0) {
            System.out.println(String.format(Locale.US, "%,d items in tree", root.count));
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
            final List<CubeDimension> groupByList = Arrays.asList(groupBy).stream().map(s -> getDimensionByName(s)).collect(Collectors.toList());

            // if we group only one item without any filters we can do so using 
            // the histogram from the dimension object.
            if (filter.isEmpty() && groupByList.size() == 1) {
                ResultNode result = new ResultNode(null, null);
                final CubeDimension dimension = groupByList.get(0);
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
            Map<CubeDimension, Filter> filterMap = filter.stream().collect(Collectors.toMap(f -> getDimensionByName(f.getKey()), f -> f));

            // find max depth we need to visit
            int maxDepth = filter.stream().mapToInt(f -> getDimensionByName(f.getKey()).depth).max().orElse(0);
            maxDepth = Math.max(maxDepth, Arrays.stream(groupBy).mapToInt(d -> getDimensionByName(d).depth).max().orElse(maxDepth));

            // aggregate element count
            ResultNode result = new ResultNode(null, null);
            result.count.set(root.count(groupByList, filterMap, result, maxDepth));

            // @todo: remove unneccesary elements
            return result;

        } finally {
            lastQueryDuration = System.currentTimeMillis() - time;
            LOG.info(String.format(Locale.US, "group by %d dimension(s) %s and %d filter(s) %s on %,d items performed in %,d ms",
                    groupBy.length, Arrays.toString(groupBy),
                    filter.size(), filter.toString(),
                    root.count, lastQueryDuration
            ));
        }

    }

    public long getLastQueryDuration() {
        return lastQueryDuration;
    }

    CubeDimension getDimensionByName(String name) {
        for (CubeDimension s : schema) {
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

    private CubeTree done() {

        // cleanup
        root.cleanup();

        // update dimensions
        schema.forEach(d -> d.getHistogram());

        System.out.println(String.format("%d items in tree", root.count));
        return this;
    }

    private static class CubeDimension extends Dimension {

        final int depth;
        private final Multiset<Object> values = HashMultiset.create();        
        private Map<Object, Integer> histogram;

        private CubeDimension(Dimension old, int depth) {
            this(old.name, depth, old.clasz);
            this.precision = old.precision;
            this.buckets = old.buckets;
            this.bucketsLessThen = old.bucketsLessThen;
        }

        private CubeDimension(String name, int depth, Class<?> clasz) {
            super(name, clasz);            
            this.depth = depth;
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

        @Override
        public String toString() {
            return String.format("Dimension:%s:%d:%s", name, depth, clasz.getName());
        }

    }

    public static class ResultNode {

        public Object value;
        public AtomicLong count;
        private final Map<Object, ResultNode> values;
        private final Dimension dimension;
        private final String name;

        private ResultNode(Dimension dimension, Object index) {
            this.dimension = dimension;
            this.name = (dimension != null) ? dimension.name : "";
            //this.value = (dimension != null) ? dimension.getKey(index) : index;
            this.value = index;
            this.count = new AtomicLong(0);
            this.values = new HashMap<>(10000);
        }

        // convienience method for testing only
        ResultNode(String name, Object value, long count) {
            this.dimension = null;
            this.name = name;
            this.value = value;
            this.count = new AtomicLong(count);
            this.values = new HashMap<>(10000);
        }

        private ResultNode(Dimension key, Object value, long count) {
            this.dimension = key;
            this.name = key.name;
            this.value = value;
            this.count = new AtomicLong(count);
            this.values = new HashMap<>(10000);
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
        synchronized ResultNode add(String key, Object value, long count) {
            ResultNode child;
            if ((child = this.values.get(value)) == null) {
                child = new ResultNode(key, value, count);
                this.values.put(value, child);
            }
            return child;
        }
        
        private synchronized ResultNode getNodeForValue(final Object key) {
            ResultNode n;
            if ((n = values.get(key)) == null) {
                n = new ResultNode(dimension, key);
                values.put(key, n);
            }
            return n;
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
        LTE,
        BETWEEN;
        
    }

    private class Cube {

        final Cube parent;
        final int depth;
        final CubeDimension dimension;
        long count = 0;
        long cumulativeCount = 0; // count of all values smaller than the current one + the current value (makes only sense if the list is sorted, e.g. not for all dimensions)

        public Cube(Cube parent, CubeDimension d, int depth) {
            this.parent = parent;
            this.dimension = d;
            this.depth = depth;
        }

        synchronized void place(List<CubeDimension> d, Object[] item, int depth) {

            // increment own count
            count++;

            // done when no dimension is left
            if (depth >= item.length) {
                return;
            }

        }

        long count(List<CubeDimension> groupBy, Map<CubeDimension, Filter> filters, ResultNode node, int maxDepth) {
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

    private class InnerNode extends Cube {

        private final Map<Object, Cube> values;

        private InnerNode(InnerNode parent, CubeDimension dim, int depth) {
            super(parent, dim, depth);
            if (dim != null && dim.isComparable()) {
                values = new TreeMap<>((o1, o2) -> {
                    if (o1 == null) {
                        return 1;
                    } else if (o2 == null) {
                        return -1;
                    }
                    return ((Comparable) o2).compareTo((Comparable) o1);
                });
            } else {
                values = new HashMap<>();
            }
        }

        @Override
        synchronized void place(List<CubeDimension> dimensions, Object[] item, int depth) {

            super.place(dimensions, item, depth);

            // store value
            final Object val = item[depth];
            Cube c;
            if ((c = values.get(val)) == null) {
                CubeDimension dim = (depth + 1 < dimensions.size()) ? dimensions.get(depth + 1) : null;
                if (dim == null) {
                    c = new Cube(this, null, depth + 1);
                } else {
                    c = new InnerNode(this, dim, depth + 1);
                }
                values.put(val, c);
            }

            // place next dimension
            c.place(dimensions, item, depth + 1);

        }

        @Override
        long count(final List<CubeDimension> groupBy,
                final Map<CubeDimension, Filter> filters,
                final ResultNode node,
                final int maxDepth) {

            if (dimension == null || depth > maxDepth || values.isEmpty()) {
                return count;
            }

            final boolean addNode = groupBy.contains(dimension);

            Filter f = filters.get(dimension);
            if (f != null) {
                // needs to be filtered
                Object key = f.getKey();                
                switch (f.getOperation()) {
                    case EQ:
                        Cube value = values.get(f.getValues().get(0));
                        if (value == null) {
                            return 0;
                        }
                        return processNode(addNode, node, groupBy, filters, maxDepth, key, value);
                    case IN:
                        return f.getValues().parallelStream().mapToLong(v -> {
                            Cube val = values.get(v);
                            if (val == null) {
                                return 0;
                            }
                            return processNode(addNode, node, groupBy, filters, maxDepth, key, val);
                        }).sum();
                    case BETWEEN:
                        
                        if (!dimension.isComparable()) {
                            throw new UnsupportedOperationException();
                        }
                        
                        TreeMap<Object, Cube> tm = (TreeMap)values;                        
                                                
                        Entry<Object,Cube> lower = tm.ceilingEntry(f.getValues().get(0));
                        Entry<Object,Cube> upper = tm.floorEntry(f.getValues().get(1));
                        
                        if (lower == null || upper == null) {
                            return 0;
                        }
                        
                        return upper.getValue().cumulativeCount - lower.getValue().cumulativeCount;                        
                    default:
                        return values.entrySet().parallelStream().filter(e -> f.getPredicate().test(e.getKey())).mapToLong(e -> {
                            return processNode(addNode, node, groupBy, filters, maxDepth, e.getKey(), e.getValue());
                        }).sum();
                }
            }

            return values.entrySet().parallelStream().mapToLong(e -> {
                return processNode(addNode, node, groupBy, filters, maxDepth, e.getKey(), e.getValue());
            }).sum();

        }

        private long processNode(final boolean addNode,
                final ResultNode node,
                final List<CubeDimension> groupBy,
                final Map<CubeDimension, Filter> filters,
                final int maxDepth,
                final Object key,
                final Cube value) {

            if (addNode) {
                final ResultNode n = node.getNodeForValue(key);
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
            long cc = 0;
            if (dimension.isComparable()) {
                for (Cube n : values.values()) {
                    cc += n.count;
                    n.cumulativeCount = cc;
                    n.cleanup();
                }
            } else {
                values.values().forEach(Cube::cleanup);
            }
        }

    }

    public static class CubeTreeBuilder {

        private final CubeTree cube;
        private final long started = System.currentTimeMillis();

        private CubeTreeBuilder(List<Dimension> schema) {
            this.cube = new CubeTree(schema);
        }

        public CubeTreeBuilder add(Object... values) {
            cube.add(values);
            return this;
        }

        public CubeTree done() {
            long duration = System.currentTimeMillis() - started;
            LOG.info(String.format(Locale.US, "done loading %,d items in %d:%02d:%02d", cube.root.count,
                    TimeUnit.MILLISECONDS.toHours(duration),
                    TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration)));
            return cube.done();
        }

    }

    public static CubeTreeBuilder build(String... schema) {
        return build(Arrays.asList(schema).stream().map(s -> new Dimension(s)).collect(Collectors.toList()));
    }

    public static CubeTreeBuilder build(Dimension... schema) {
        final List<Dimension> asList = Arrays.asList(schema);
        asList.forEach(d -> {
            if (d.clasz.isPrimitive()) {
                throw new IllegalArgumentException(String.format("primitives like '%s' not allowed", d.clasz));
            }
            if (d == null) {
                throw new IllegalArgumentException("dimensions cannot be null");
            }
        });
        return build(asList);
    }

    public static CubeTreeBuilder build(List<Dimension> schema) {
        return new CubeTreeBuilder(schema);
    }

    public static CubeTree build(Result<Record> records, List<String> schema) {
        CubeTree cube = new CubeTree(schema.stream().map(s -> new Dimension(s)).collect(Collectors.toList()));
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
