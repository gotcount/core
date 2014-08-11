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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import static org.fest.assertions.api.Assertions.*;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sebastian
 */
public class JTreeCubeTest {

    private static Connection conn;
    private static JTreeCube cube;
    private static JTreeCube cubeXL;

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
        
        cube = getSimpleCube();
        cubeXL = getLargeCube();
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            conn.close();
        } catch (SQLException ex) {
            // ignore
        }
    }

    private static JTreeCube getSimpleCube() {
        
        return JTreeCube.build(Arrays.asList("time", "client", "path"))
                .add("2014-01-01 10:57:48", "127.0.0.1", "/")
                .add("2014-01-01 10:58:48", "127.0.0.1", "/")
                .add("2014-01-01 11:58:35", "146.140.3.24", "/test")
                .add("2014-01-01 12:57:15", "127.0.0.1", "/test/code")
                .add("2014-01-06 18:57:04", "127.0.0.1", "/")
                .add("2014-01-06 18:57:04", "127.0.0.1", "/123")
                .done();
    }
    
    private static Set<String> ip = new HashSet<>();
    private static Set<String> path = new HashSet<>();
    private static long largeCubeSize = 1000 * 500;
        
    private static JTreeCube getLargeCube() {
        
        JTreeCube.JTreeCubeBuilder builder = JTreeCube.build(Arrays.asList("time", "client", "path"));
        
        while(JTreeCubeTest.ip.size() < 50) {
            JTreeCubeTest.ip.add(randomIp());
        }
        while (JTreeCubeTest.path.size() < 50) {
            JTreeCubeTest.path.add(randomPath());
        }
        
        List<String> ip = new ArrayList<>(JTreeCubeTest.ip);
        List<String> path = new ArrayList<>(JTreeCubeTest.path);
        
        for (int i = 0; i < largeCubeSize; i++) {
            builder.add(JTreeCubeTest.randomDate(), 
                    ip.get(JTreeCubeTest.r.nextInt(ip.size())), 
                    path.get(JTreeCubeTest.r.nextInt(path.size())));
        }
        
        return builder.done();
    }
    
    static Random r = new Random();
    static SimpleDateFormat dt = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss"); 
    
    private static String randomDate() {        
        return dt.format(new Date(Math.abs(System.currentTimeMillis() + (int)(r.nextGaussian() * 1000 * 60 * 60 * 24 * 30))));
    }
    
    private static int gaussianInt(int max) {
        int val;
        while ((val = (int)(r.nextGaussian() * max)) > max || val < 0) {            
        }
        return val;
    }
    
    private static String randomIp() {
        return gaussianInt(256) + "." + gaussianInt(256) + "." + gaussianInt(256) + "." + gaussianInt(256);
    }
    
    static List<String> paths = Arrays.asList("user", "home", "action", "client");
    
    private static String randomPath() {
        int length = r.nextInt(4);
        StringBuilder str = new StringBuilder("/");
        for (int i = 0; i < length; i++) {
            str.append(paths.get(r.nextInt(paths.size())));
            str.append("/");
        }
        return str.toString();
    }
    
    @Test
    public void largeCubeClient() {
        JTreeCube.ResultNode actual = cubeXL.count("client");
        assertThat(actual.count.get()).isEqualTo(largeCubeSize);
    }
    
    @Test
    public void largeCubePath() {
        cubeXL.count("path");
    }
    
    @Test
    public void largeCubeTime() {
        cubeXL.count("time");
    }
 
    @Test
    public void simpleCubeSize() {
        assertThat(cube.size()).isEqualTo(6);
    }

    @Test
    public void countSimpleCubeNoFilter() {
        assertThat(cube.count()).isEqualTo(new JTreeCube.ResultNode("", null, 6));
    }
    
    @Test
    public void countSimpleCubeByClientPath() {
        JTreeCube.ResultNode expected = new JTreeCube.ResultNode("", null, 6);
        JTreeCube.ResultNode c127 = expected.add("client", "127.0.0.1", 5);
        c127.add("path", "/", 3);
        c127.add("path", "/123", 1);
        c127.add("path", "/test/code", 1);
        JTreeCube.ResultNode c146 = expected.add("client", "146.140.3.24", 1);
        c146.add("path", "/test", 1);
        
        JTreeCube.ResultNode actual = cube.count("client", "path");
        
        System.out.println("__cube");
        System.out.println(cube.toString());
        
        System.out.println("__expected");
        System.out.println(expected);
        System.out.println("__actual");
        System.out.println(actual);
                
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void countSimpleCubeByClient() {
        JTreeCube.ResultNode expected = new JTreeCube.ResultNode("", null, 6);
        expected.add("client", "127.0.0.1", 5);
        expected.add("client", "146.140.3.24", 1);
        
        assertThat(cube.count("client")).isEqualTo(expected);
    }
    
    @Test
    public void countSimpleCubeByPath() {
        JTreeCube.ResultNode expected = new JTreeCube.ResultNode("", null, 6);
        expected.add("path", "/", 3);
        expected.add("path", "/test", 1);
        expected.add("path", "/test/code", 1);
        expected.add("path", "/123", 1);
        
        assertThat(cube.count("path")).isEqualTo(expected);
    }
    
    @Test
    public void preventInvertedCount() {
        try {
            cube.count("path", "time");
            fail("missing exception");
        } catch (UnsupportedOperationException ex) {
        } catch (Exception ex) {
            fail("wrong exception");
        }        
    }
    
    @Test
    public void countSimpleCubeByPathWithClientFilter() {
        JTreeCube.ResultNode expected = new JTreeCube.ResultNode("", null, 3);
        expected.add("client", "127.0.0.1", 3);
        expected.add("client", "146.140.3.24", 0);
        
        List<Filter> filter = new ArrayList<>();
        filter.add(new Filter("path", JTreeCube.Operation.EQ, "/"));
        
        JTreeCube.ResultNode actual = cube.count(filter, "client");
        
        
        
        assertThat(actual).isEqualTo(expected);
    }
   
    @Test
    public void testBuildFromJooq() {

        DSLContext create = DSL.using(conn, SQLDialect.MYSQL);
        Result<Record> result = create.select().from("accesslog").limit(100).fetch();
        JTreeCube cube = JTreeCube.build(result, Arrays.asList("path", "timestamp", "hostname"));
        
        JTreeCube.ResultNode expected = new JTreeCube.ResultNode("", null, 100);
        expected.add("path", "comment/reply/72", 24);
        expected.add("path", "comment/reply/16", 17);
        expected.add("path", "comment/reply/2", 16);
        expected.add("path", "comment/reply/43", 11);
        expected.add("path", "node/add/forum/1", 9);
        expected.add("path", "user", 7);
        expected.add("path", "forum/1", 6);
        expected.add("path", "forum", 4);
        expected.add("path", "node/2", 1);
        expected.add("path", "node/586", 1);
        expected.add("path", "node/72", 1);
        expected.add("path", "node/36", 1);
        expected.add("path", "node/16", 1);
        expected.add("path", "blog/feed", 1);
        
        assertThat(cube.size()).isEqualTo(100);
        assertThat(cube.count("path")).isEqualTo(expected);
        
    }

}
