/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
class DbSchemaBuilder extends SchemaBuilder {

    private final Connection connection;
    private final String table;
    private final SQLDialect dialect;

    DbSchemaBuilder(Connection conn, String table, SQLDialect dialect) {
        this.connection = conn;
        this.table = table;
        this.dialect = dialect;

        readTable();
    }

    DbSchemaBuilder(Connection conn, String table) {
        this(conn, table, SQLDialect.MYSQL);
    }

    private void readTable() {

        DSLContext create = DSL.using(connection, dialect);
        Cursor<Record> fetchLazy = create.resultQuery(String.format("SELECT * FROM `%s`;", table)).fetchLazy();

        // add dimensions
        int index = 0;
        for (Field f : fetchLazy.fields()) {
            dimensions.put(f.getName(), new BitMapDimension(f.getName(), index++, f.getType()));
        }

        // add data
        fetchLazy.forEach(r -> add(r.intoArray()));

    }

}
