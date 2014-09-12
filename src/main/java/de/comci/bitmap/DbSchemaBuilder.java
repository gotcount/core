/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.sql.Connection;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
class DbSchemaBuilder extends SchemaBuilder {

    private final Connection connection;
    private final String table;

    DbSchemaBuilder(Connection conn, String table) {
        this.connection = conn;
        this.table = table;
        
        readTable();        
    }

    private void readTable() {
        
        DSLContext create = DSL.using(connection, SQLDialect.MYSQL);
        Cursor<Record> fetchLazy = create.resultQuery(String.format("SELECT * FROM `%s`;", table)).fetchLazy();

        int index = 0;
        for (Field f : fetchLazy.fields()) {
            dimensions.put(f.getName(), new Dimension(f.getName(), index++, f.getType()));
        }
        
        final BitMapColumns map = get();
        
        fetchLazy.forEach(r -> map.add(r.intoArray()));
        
    }
    
}
