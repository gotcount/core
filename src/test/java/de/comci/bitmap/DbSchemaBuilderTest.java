/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.fest.assertions.api.Assertions.*;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class DbSchemaBuilderTest {

    public DbSchemaBuilderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    MockDataProvider provider;
    MockConnection connection;
    DSLContext create;

    @Before
    public void setUp() {
        // Initialise your data provider (implementation further down):
        provider = new MyProvider();
        connection = new MockConnection(provider);

        // Pass the mock connection to a jOOQ DSLContext:
        create = DSL.using(connection, SQLDialect.MYSQL);

    }

    @After
    public void tearDown() {
    }

    @Test
    public void dimensionsReadCorrectly() {

        DbSchemaBuilder instance = new DbSchemaBuilder(connection, "test", SQLDialect.MYSQL);

        assertThat(instance.get().getDimensions()).containsOnly(
                new BitMapDimension("name", 0, String.class),
                new BitMapDimension("gender", 1, String.class),
                new BitMapDimension("age", 2, Integer.class)
        );

    }
    
    @Test
    public void allRowsAdded() {
        
        DbSchemaBuilder instance = new DbSchemaBuilder(connection, "test", SQLDialect.MYSQL);
        assertThat(instance.get().size()).isEqualTo(4);
        
    }
    
    @Test
    public void actuallyWorks() {
        
        Multiset<Value> hist = HashMultiset.create();
        hist.add(Value.get("a name"), 3);
        hist.add(Value.get("b name"), 1);
        
        DbSchemaBuilder instance = new DbSchemaBuilder(connection, "test", SQLDialect.MYSQL);
        assertThat(instance.get().histogram("name")).isEqualTo(hist);        
        
    }

    /**
     * http://www.jooq.org/doc/3.4/manual/tools/jdbc-mocking/
     */
    private static class MyProvider implements MockDataProvider {

        @Override
        public MockResult[] execute(MockExecuteContext ctx) throws SQLException {

            // You might need a DSLContext to create org.jooq.Result and org.jooq.Record objects
            DSLContext create = DSL.using(SQLDialect.MYSQL);
            MockResult[] mock = new MockResult[1];

            Field<String> name = DSL.fieldByName(String.class, "name");
            Field<String> gender = DSL.fieldByName(String.class, "gender");
            Field<Integer> age = DSL.fieldByName(Integer.class, "age");

            // Always return one author record
            Result<Record3<String, String, Integer>> result = create.newResult(name, gender, age);

            List<List<Object>> data = Arrays.asList(
                    Arrays.asList("a name", "m", 19),
                    Arrays.asList("b name", "f", 26),
                    Arrays.asList("a name", "f", 15),
                    Arrays.asList("a name", "m", 31)
            );
            
            int i = 0;
            for (List<Object> d : data) {
                result.add(create.newRecord(name, gender, age));
                result.get(i).setValue(name, (String)d.get(0));
                result.get(i).setValue(gender, (String)d.get(1));
                result.get(i).setValue(age, (int)d.get(2));
                i++;
            }
          
            mock[0] = new MockResult(1, result);

            return mock;

        }
    }

}
