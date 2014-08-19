/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.jtreecube;

import de.comci.parser.apache.ApacheLogRecord;
import de.comci.parser.apache.ApacheParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Sebastian
 */
public class App {

    public static void main(String[] args) {

        try {
            JTreeCube.JTreeCubeBuilder b = JTreeCube.build(                                        
                    new JTreeCube.Dimension("status", Short.class),
                    new JTreeCube.Dimension("method", String.class),
                    new JTreeCube.Dimension("date", Date.class),
                    new JTreeCube.Dimension("request", String.class),
                    new JTreeCube.Dimension("bytes", Integer.class));
            
            Stream<ApacheLogRecord> log = ApacheParser.parse(new File("D:\\Downloads\\NASA_access_log_Aug95\\access_log_Aug95"));
            log.forEach(l -> b.add(l.statusCode, l.method, l.date, l.request, l.bytesSent));            
            JTreeCube done = b.done();
            
            System.out.println(done.count("status"));
            System.out.println(done.count("method"));
            done.count("date");
            done.count("request");
            System.out.println(done.count("status", "method"));
            done.count("date", "bytes");
            
            System.out.println(done.count(Arrays.asList(new JTreeCube.Filter("method", JTreeCube.Operation.EQ, "GET")), "status"));
            System.out.println(done.count(Arrays.asList(new JTreeCube.Filter("bytes", JTreeCube.Operation.LTE, 10)), "status"));
            System.out.println(done.count(Arrays.asList(new JTreeCube.Filter("bytes", JTreeCube.Operation.LTE, 10),
                                                        new JTreeCube.Filter("method", JTreeCube.Operation.IN, "POST")), "status"));
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}