/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.jtreecube;

import de.comci.jtreecube.JTreeCube.Filter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import static org.fest.assertions.api.Assertions.*;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Sebastian
 */
public class JTreeCubeTest {

    private static Connection conn;

    public JTreeCubeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        conn = null;

        String userName = "root";
        String password = "";
        String url = "jdbc:mysql://localhost:3306/d012dad2";

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, userName, password);
        } catch (Exception e) {
            // For the sake of this tutorial, let's keep exception handling simple
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            conn.close();
        } catch (SQLException ex) {
            // ignore
        }
    }

    private JTreeCube getSimpleCube() {
        
        return JTreeCube.build(Arrays.asList("time", "client", "path"))
                .add("2014-01-01 10:57:48", "127.0.0.1", "/")
                .add("2014-01-01 10:58:48", "127.0.0.1", "/")
                .add("2014-01-01 11:58:35", "146.140.3.24", "/test")
                .add("2014-01-01 12:57:15", "127.0.0.1", "/test/code")
                .add("2014-01-06 18:57:04", "127.0.0.1", "/")
                .add("2014-01-06 18:57:04", "127.0.0.1", "/123")
                .done();
    }
 
    @Test
    public void simpleCubeSize() {
        JTreeCube cube = getSimpleCube();
        assertThat(cube.size()).isEqualTo(6);
    }

    @Test
    public void countSimpleCubeNoFilter() {
        JTreeCube cube = getSimpleCube();
        assertThat(cube.count()).isEqualTo(new JTreeCube.ResultNode("", null, 6));
    }
    
    @Test
    public void countSimpleCubeByClientPath() {
        JTreeCube cube = getSimpleCube();
        JTreeCube.ResultNode expected = new JTreeCube.ResultNode("", null, 6);
        JTreeCube.ResultNode c127 = expected.add("client", "127.0.0.1", 5);
        c127.add("path", "/", 3);
        c127.add("path", "/123", 1);
        c127.add("path", "/test/code", 1);
        JTreeCube.ResultNode c146 = expected.add("client", "146.140.3.24", 1);
        c146.add("path", "/test", 1);
        
        System.out.println(cube.count("client"));
        System.out.println(expected);
        
        assertThat(cube.count("client", "path")).isEqualTo(expected);
    }

    @Test
    public void countSimpleCubeByClient() {
        JTreeCube cube = getSimpleCube();
        JTreeCube.ResultNode expected = new JTreeCube.ResultNode("", null, 6);
        expected.add("client", "127.0.0.1", 5);
        expected.add("client", "146.140.3.24", 1);
        
        assertThat(cube.count("client")).isEqualTo(expected);
    }
    
    @Test
    public void countSimpleCubeByPath() {
        JTreeCube cube = getSimpleCube();
        JTreeCube.ResultNode expected = new JTreeCube.ResultNode("", null, 6);
        expected.add("path", "/", 3);
        expected.add("path", "/test", 1);
        expected.add("path", "/test/code", 1);
        expected.add("path", "/123", 1);
        
        System.out.println(cube.count("path"));
        System.out.println(expected);
        
        assertThat(cube.count("path")).isEqualTo(expected);
    }

    @Test
    @Ignore
    public void testBuildFromJooq() {

        DSLContext create = DSL.using(conn, SQLDialect.MYSQL);
        Result<Record> result = create.select().from("accesslog").limit(1000).fetch();
        JTreeCube cube = JTreeCube.build(result, Arrays.asList("path", "timestamp", "hostname"));
        
        assertThat(cube.size()).isEqualTo(1000);

        Stream<JTreeCube.Pair<String, Long>> a = cube.aggregate("hostname", SortOrder.DESC, new ArrayList<Filter>());

        assertThat((long) a.findFirst().get().value).isEqualTo((long) 284);

    }

}
