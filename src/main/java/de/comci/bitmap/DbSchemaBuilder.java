/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
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
public class DbSchemaBuilder extends SchemaBuilder {

    private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(DbSchemaBuilder.class);

    private final Connection connection;
    private final String table;
    private final SQLDialect dialect;
    private final List<Column> inputColumns;

    public DbSchemaBuilder(Connection conn, String table) {
        this(conn, table, SQLDialect.MYSQL);
    }

    public DbSchemaBuilder(Connection conn, String table, String... columns) {
        this(conn, table, SQLDialect.MYSQL, columns);
    }

    public DbSchemaBuilder(Connection conn, String table, Column... columns) {
        this(conn, table, SQLDialect.MYSQL, columns);
    }

    public DbSchemaBuilder(Connection conn, String table, SQLDialect dialect) {
        this(conn, table, SQLDialect.MYSQL, "*");
    }

    public DbSchemaBuilder(Connection conn, String table, SQLDialect dialect, String... columns) {
        this.connection = conn;
        this.table = table;
        this.dialect = dialect;
        this.inputColumns = Arrays.asList(columns).stream().map(s -> new Column(s.trim(), null)).collect(Collectors.toList());

        readTable();
    }

    public DbSchemaBuilder(Connection conn, String table, SQLDialect dialect, Column... columns) {
        this.connection = conn;
        this.table = table;
        this.dialect = dialect;
        this.inputColumns = Arrays.asList(columns);

        readTable();
    }

    private void readTable() {

        DSLContext create = DSL.using(connection, dialect);

        LOG.trace(String.format("connection to db established '%s'", connection.toString()));

        Result<Record> fetch = create.selectFrom(DSL.tableByName(table)).limit(1).fetch();

        List<Field<?>> fieldsToFetch = new ArrayList<>();

        Map<String, Column> columnsMap = inputColumns.stream().collect(Collectors.toMap(Column::getName, c -> c));

        // add dimensions
        int index = 0;
        for (Field f : fetch.fields()) {
            Column c = null;
            if (inputColumns.isEmpty() || inputColumns.get(0).name.equals("*") || (c = columnsMap.get(f.getName())) != null) {
                Class type = (c != null && c.type != null) ? c.type : f.getType();
                dimensions.put(f.getName(), new BitMapDimension(f.getName(), index++, type));
                fieldsToFetch.add(f);
            }
        }

        LOG.trace(String.format("%d dimensions read from table '%s'", dimensions.size(), table));

        // add data
        Cursor<Record> fetchLazy = create.select(fieldsToFetch).from(DSL.tableByName(table)).fetchLazy();
        fetchLazy.forEach(r -> {
            Object[] oa = new Object[fieldsToFetch.size()];
            int i = 0;
            for (Field f : fieldsToFetch) {
                Column c = columnsMap.get(f.getName());
                if (c != null && c.type != null) {
                    oa[i++] = c.map(r.getValue((Field<?>) f, c.type), c.type);
                } else {
                    oa[i++] = r.getValue(f);
                }
            }
            add(oa);
        });

        LOG.trace(String.format("%d rows read from table '%s'", size(), table));

    }

    public static class Column {

        private final static Set<Integer> DATE_PRECISION = new HashSet<>(Arrays.asList(1, 2, 5, 11, 12, 13, 14));

        final String name;
        final Class type;
        final Double precision;

        public Column(String name, Class type) {
            this(name, type, null);
        }

        public Column(String name, Class type, Double precision) {
            this.name = name;
            this.type = type;
            this.precision = precision;

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
                return map((Date)o);
            } else if (Long.class.isAssignableFrom(clasz)) {
                return map((Long)o);
            } else if (Integer.class.isAssignableFrom(clasz)) {
                return map(((Integer)o).longValue());
            } else if (Float.class.isAssignableFrom(clasz)) {
                return map(((Float)o).doubleValue());
            } else if (Double.class.isAssignableFrom(clasz)) {
                return map((Double)o);
            } else if (String.class.isAssignableFrom(clasz)) {
                return map((String)o);
            }
            throw new IllegalArgumentException(String.format("Type '%s' is not currently not supported by map function", clasz.toString()));
        }

        Date map(Date d) {
            if (precision == null || precision == 0 || d == null) {
                return d;
            }
            return DateUtils.truncate(d, precision.intValue());
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
