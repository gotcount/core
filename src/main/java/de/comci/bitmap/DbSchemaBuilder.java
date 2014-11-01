/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
                    oa[i++] = r.getValue((Field<?>)f, c.type);
                } else {
                    oa[i++] = r.getValue(f);
                }
            }
            add(oa);
        });

        LOG.trace(String.format("%d rows read from table '%s'", size(), table));

    }

    public static class Column {

        final String name;
        final Class type;

        public Column(String name, Class type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Class getType() {
            return type;
        }
        
    }

}
