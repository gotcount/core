/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SelectJoinStep;
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
    private final List<String> inputColumns;
    
    DbSchemaBuilder(Connection conn, String table) {
        this(conn, table, SQLDialect.MYSQL);
    }
    
    DbSchemaBuilder(Connection conn, String table, String... columns) {
        this(conn, table, SQLDialect.MYSQL, columns);
    }

    DbSchemaBuilder(Connection conn, String table, SQLDialect dialect) {
        this(conn, table, SQLDialect.MYSQL, "*");
    }

    DbSchemaBuilder(Connection conn, String table, SQLDialect dialect, String... columns) {
        this.connection = conn;
        this.table = table;
        this.dialect = dialect;
        this.inputColumns = Arrays.asList(columns).stream().map(s -> s.trim()).collect(Collectors.toList());

        readTable();
    }

    private void readTable() {

        DSLContext create = DSL.using(connection, dialect);

        LOG.trace(String.format("connection to db established '%s'", connection.toString()));

        Result<Record> fetch = create.selectFrom(DSL.tableByName(table)).limit(1).fetch();

        Collection<Field<?>> fieldsToFetch = new HashSet<>();
        
        // add dimensions
        int index = 0;
        for (Field f : fetch.fields()) {
            if (inputColumns.isEmpty() || inputColumns.get(0).equals("*") || inputColumns.contains(f.getName())) {
                dimensions.put(f.getName(), new BitMapDimension(f.getName(), index++, f.getType()));
                fieldsToFetch.add(f);
            }
        }
        
        LOG.trace(String.format("%d dimensions read from table '%s'", dimensions.size(), table));
        
        // add data
        Cursor<Record> fetchLazy = create.select(fieldsToFetch).from(DSL.tableByName(table)).fetchLazy();
        fetchLazy.forEach(r -> add(r.intoArray()));

        LOG.trace(String.format("%d rows read from table '%s'", size(), table));

    }

}
