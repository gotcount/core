/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DateUtils;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class JooqDimensionBuilder extends DefaultDimensionBuilder {

    private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(JooqDimensionBuilder.class);

    private final Connection connection;
    private final String table;
    private final Map<String, Column> availableFields;
    private final DSLContext ctx;

    public JooqDimensionBuilder(Connection conn, String table) {
        this(conn, table, SQLDialect.MYSQL);
    }

    public JooqDimensionBuilder(Connection conn, String table, SQLDialect dialect) {
        this.connection = conn;
        this.table = table;

        // db context
        ctx = DSL.using(connection, dialect);

        // collect fields from table
        availableFields = new LinkedHashMap<>();
        Result<Record> singleRow = ctx.selectFrom(DSL.tableByName(table)).limit(1).fetch();
        for (Field f : singleRow.fields()) {
            availableFields.put(f.getName(), new Column(f.getName(), f.getType(), f));
        }

        LOG.trace(String.format("%d columns found in table '%s'", availableFields.size(), table));
    }

    @Override
    public DimensionBuilder dimension(String name, Class type) {
        return dimension(name, type, null);
    }

    @Override
    public DimensionBuilder dimension(String name, Class type, Double precision) {
        return dimension(name, name, type, precision);
    }

    public JooqDimensionBuilder dimension(String name, String alias, Class type, Double precision) {
        // only existing dimension from table can be added
        if (availableFields.containsKey(name)) {
            availableFields.put(name, new Column(name, (type != null) ? type : availableFields.get(name).getType(), precision, availableFields.get(name).field));
            return (JooqDimensionBuilder) super.dimension(name, availableFields.get(name).getType(), precision);
        }
        throw new IllegalArgumentException("not an existing dimension");
    }

    @Override
    public CollectionBuilder getCollectionBuilder() {

        if (builder == null) {
            // no specific dimensions set
            if (super.dimensions.isEmpty()) {
                availableFields.entrySet().forEach(e -> dimension(e.getKey(), e.getValue().getType()));
            }
            builder = super.getCollectionBuilder();

            List<Field<?>> fieldsToFetch = availableFields.entrySet()
                    .stream()
                    // filter those fields being selected as dimensions
                    .filter(e -> super.dimensions.keySet().contains(e.getKey()))
                    // ensure field ordering according to the order the dimensions have been defined
                    .sorted((a, b) -> super.dimensions.get(a.getKey()).index - super.dimensions.get(b.getKey()).index)
                    .map(e -> e.getValue().field)
                    .collect(Collectors.toList());

            long rows = 0;
            Cursor<Record> fetchLazy = ctx.select(fieldsToFetch).from(DSL.tableByName(table)).fetchLazy();
            for (Record r : fetchLazy) {
                Object[] oa = new Object[fieldsToFetch.size()];
                int i = 0;
                for (Field f : fieldsToFetch) {
                    Column c = availableFields.get(f.getName());
                    if (c != null && c.type != null) {
                        oa[i++] = c.map(r.getValue((Field<?>) f, c.type), c.type);
                    } else {
                        oa[i++] = r.getValue(f);
                    }
                }
                builder = add(oa);
                rows++;
            }

            LOG.trace(String.format("%d rows read from table '%s'", rows, table));
        }
        return builder;
    }

    protected static class Column {

        private final static Set<Integer> DATE_PRECISION = new HashSet<>(Arrays.asList(1, 2, 5, 11, 12, 13, 14));

        final String name;
        final Class type;
        final Double precision;
        final Field<?> field;

        public Column(String name, Class type, Field<?> field) {
            this(name, type, null, field);
        }

        public Column(String name, Class type, Double precision, Field<?> field) {
            this.name = name;
            this.type = type;
            this.precision = precision;
            this.field = field;

            // only certain numbers are valid for date precision
            if (precision != null
                    && Date.class.isAssignableFrom(type)
                    && !DATE_PRECISION.contains(precision.intValue())) {
                throw new IllegalArgumentException(
                        String.format(
                                "date precision can only be one of: %s",
                                DATE_PRECISION.toString()
                        )
                );
            }
        }

        public String getName() {
            return name;
        }

        public Class getType() {
            return type;
        }

        public Double getPrecision() {
            return precision;
        }

        Object map(Object o, Class clasz) {
            if (Date.class.isAssignableFrom(clasz)) {
                return map((Date) o);
            } else if (Integer.class.isAssignableFrom(clasz)) {
                return map(((Integer) o).intValue());
            } else if (Long.class.isAssignableFrom(clasz)) {
                return map((Long) o);
            } else if (Float.class.isAssignableFrom(clasz)) {
                return map(((Float) o).doubleValue());
            } else if (Double.class.isAssignableFrom(clasz)) {
                return map((Double) o);
            } else if (String.class.isAssignableFrom(clasz)) {
                return map((String) o);
            }
            throw new IllegalArgumentException(String.format("Type '%s' is not currently not supported by map function", clasz.toString()));
        }

        Date map(Date d) {
            if (precision == null || precision == 0 || d == null) {
                return d;
            }
            return DateUtils.truncate(d, precision.intValue());
        }

        Integer map(Integer l) {
            if (l == null) {
                return l;
            }
            return map(1.0 * l).intValue();
        }

        Long map(Long l) {
            if (l == null) {
                return l;
            }
            return map(1.0 * l).longValue();
        }

        Double map(Double d) {
            if (precision == null || precision == 0 || precision == 1 || d == null) {
                return d;
            }
            return (Math.floor(d / precision) * precision);
        }

        String map(String s) {

            if (precision == null || precision == 0 || s == null) {
                return s;
            }

            if (precision > 0) {
                return s.substring(0, Math.min(s.length(), precision.intValue()));
            } else if (precision < 0) {
                return s.substring(s.length() - Math.min(s.length(), -precision.intValue()));
            }

            return s;
        }

    }

}
