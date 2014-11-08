/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import de.comci.histogram.HashHistogram;
import de.comci.histogram.Histogram;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.fest.assertions.api.Assertions.*;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
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

    MyProvider provider;
    MockConnection connection;
    DSLContext context;

    @Before
    public void setUp() {
        // Initialise your data provider (implementation further down):
        provider = new MyProvider();
        connection = new MockConnection(provider);

        // Pass the mock connection to a jOOQ DSLContext:
        context = DSL.using(connection, SQLDialect.MYSQL);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void dimensionsReadCorrectly() {

        provider.addField("name", String.class)
                .addField("gender", String.class)
                .addField("age", Integer.class)
                .addRow("a name", "male", 25); 
        
        DbSchemaBuilder instance = new DbSchemaBuilder(connection, "test", SQLDialect.MYSQL);
        
        assertThat(instance.build().getDimensions()).containsOnly(
                new BitMapDimension("name", 0, String.class),
                new BitMapDimension("gender", 1, String.class),
                new BitMapDimension("age", 2, Integer.class)
        );

    }
    
    @Test
    public void someDimensionsReadCorrectly() {

        provider.addField("name", String.class)
                .addField("gender", String.class)
                .addField("age", Integer.class)
                .addField("email", String.class)
                .addRow("a name", "male", 25, "1@2.3")
                .addRow("another name", "female", 215, "1@2.3")
                .addRow("a name", "male", 25, "1@2.3");
        
        DbSchemaBuilder instance = new DbSchemaBuilder(connection, "test", SQLDialect.MYSQL, "name", "age", "gender");

        assertThat(instance.build().getDimensions()).containsOnly(
                new BitMapDimension("name", 0, String.class),
                new BitMapDimension("age", 1, Integer.class),
                new BitMapDimension("gender", 2, String.class)
        );
        
        assertThat(instance.build().size()).isEqualTo(3);
        assertThat(instance.build().count("name", "a name")).isEqualTo(2);
        assertThat(instance.build().count("age", 25)).isEqualTo(2);

    }
    
    @Test
    public void allRowsAdded() {
        
        provider.addField("name", String.class)
                .addField("gender", String.class)
                .addField("age", Integer.class)
                .addField("email", String.class)
                .addRow("a name", "male", 1, "1@2")
                .addRow("a name", "female", 1, "1@3")
                .addRow("a name", "unknown", 2, "1@5")
                .addRow("another name", "male", 3, "1@8");
        
        DbSchemaBuilder instance = new DbSchemaBuilder(connection, "test", SQLDialect.MYSQL);
        assertThat(instance.build().size()).isEqualTo(4);
        
    }
    
    @Test
    public void actuallyWorks() {
        
        provider.addField("name", String.class)
                .addField("gender", String.class)
                .addField("age", Integer.class)
                .addField("email", String.class)
                .addRow("a name", "male", 1, "1@2")
                .addRow("a name", "female", 1, "1@3")
                .addRow("a name", "unknown", 2, "1@5")
                .addRow("b name", "male", 3, "1@8")
                .addRow("b name", "male", 3, "1@8");
        
        Histogram<Value> hist = new HashHistogram<>();
        hist.set(Value.get("a name"), 3);
        hist.set(Value.get("b name"), 2);
        
        DbSchemaBuilder instance = new DbSchemaBuilder(connection, "test", SQLDialect.MYSQL);
        assertThat(instance.build().histogram("name")).isEqualTo(hist);        
        
    }
    
    @Test
    public void mapDatePrecisionNull() {
        
        DbSchemaBuilder.Column c = new DbSchemaBuilder.Column("test", String.class, null);
        Date d = new Date(109,0,1);
        assertThat(c.map(d)).isSameAs(d);
    }
    
    @Test
    public void mapDatePrecisionValid() {
       
        // valid values are: 1,2,5,11,12,13,14
        
        DbSchemaBuilder.Column c = new DbSchemaBuilder.Column("test", String.class, 1.0);
        Date d = new Date(109,0,12);
        assertThat(c.map(d)).isEqualTo(new Date(109,0,1));
        
        c = new DbSchemaBuilder.Column("test", String.class, 2.0);
        d = new Date(109,4,12);
        assertThat(c.map(d)).isEqualTo(new Date(109,4,1));
        
        c = new DbSchemaBuilder.Column("test", String.class, 5.0);
        d = new Date(109,4,12,5,6);
        assertThat(c.map(d)).isEqualTo(new Date(109,4,12,0,0));
        
        c = new DbSchemaBuilder.Column("test", String.class, 11.0);
        d = new Date(109,4,12,5,6);
        assertThat(c.map(d)).isEqualTo(new Date(109,4,12,5,0));
        
        c = new DbSchemaBuilder.Column("test", String.class, 13.0);
        d = getDate(2009,4,12,5,6,54,123);
        assertThat(c.map(d)).isEqualTo(getDate(2009, 4, 12, 5, 6, 54, 0));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void illegalDatePrecision() {
        DbSchemaBuilder.Column column = new DbSchemaBuilder.Column("12", Date.class, 0.0);
    }
    
    @Test
    public void readWithLimitedPrecision() {

        provider.addField("date", java.sql.Date.class)
                .addRow(getDate(2012, 1, 2, 16, 24, 28, 564))
                .addRow(getDate(2012, 1, 4, 16, 21, 28, 564))
                .addRow(getDate(2012, 1, 4, 12, 35, 51, 564)); 
        
        DbSchemaBuilder instance = new DbSchemaBuilder(
                connection, 
                "test", 
                SQLDialect.MYSQL, 
                new DbSchemaBuilder.Column("date", Date.class, 5.0)
        );
        
        final BitMapCollection collection =  instance.build();
        
        assertThat(collection.getDimensions()).containsOnly(
                new BitMapDimension("date", 0, Date.class)
        );
        
        Histogram<Value> expected = new HashHistogram<>();
        expected.set(new Value(getDate(2012, 1, 4, 0, 0, 0, 0), Date.class), 2);
        expected.set(new Value(getDate(2012, 1, 2, 0, 0, 0, 0), Date.class), 1);
        
        assertThat(collection.histogram("date")).isEqualTo(expected);

    }
    
    private Date getDate(int year, int month, int day, int hour, int minute, int second, int ms) {
        Calendar i = Calendar.getInstance();    
        i.set(year, month, day, hour, minute, second);
        i.set(Calendar.MILLISECOND, ms);
        return i.getTime();
    }
    
    /**
     * http://www.jooq.org/doc/3.4/manual/tools/jdbc-mocking/
     */
    private static class MyProvider implements MockDataProvider {

        List<Field<?>> fields = new ArrayList<>();
        List<Object[]> data = new ArrayList<>();
        
        public MyProvider() {            
        }
        
        MyProvider addField(String name, Class type) {
            fields.add(DSL.fieldByName(type, name));
            return this;
        }
        
        MyProvider addRow(Object... row) {
            if (row.length != fields.size())
                throw new IllegalArgumentException("row size does not match column count");
            data.add(row);
            return this;
        }
        
        @Override
        public MockResult[] execute(MockExecuteContext ctx) throws SQLException {

            // You might need a DSLContext to create org.jooq.Result and org.jooq.Record objects
            DSLContext context = DSL.using(SQLDialect.MYSQL);
            MockResult[] mock = new MockResult[1];

            if (!ctx.sql().startsWith("select * from")) {
                Pattern p = Pattern.compile("select ([a-z]+)(, ([a-z]+))* from `test`.*");
                Matcher matcher = p.matcher(ctx.sql());
                if (matcher.matches()) {
                    for (int i = 0; i < matcher.groupCount(); i++) {
                        System.out.println(String.format("%d: %s", i, matcher.group(i)));
                    }
                }
            }
            
            final Field[] fieldArray = fields.stream().toArray(size -> new Field[size]);

            // Result object
            Result result = context.newResult(fieldArray);

            for (Object[] row : data) {
                final Record newRecord = context.newRecord(fieldArray);
                result.add(newRecord);
                for (int j = 0; j < fields.size(); j++) {
                    newRecord.<Object>setValue((Field<Object>)fields.get(j), (Object)row[j]);
                }                
            }
                                  
            mock[0] = new MockResult(1, result);

            return mock;

        }
    }

}
