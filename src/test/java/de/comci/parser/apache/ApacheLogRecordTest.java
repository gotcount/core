/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.parser.apache;

import static org.fest.assertions.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class ApacheLogRecordTest {
 
    @Test
    public void recordType() {
        
        ApacheLogRecord r = new ApacheLogRecord(null, null, null, null, null, "/sdfdsf/dfsdfdf/df.txt", null, 0, 0);
        assertThat(r.getRequestType()).isEqualTo("document");
        
        r = new ApacheLogRecord(null, null, null, null, null, "/sdfdsf/dfsdfdf/df.gif", null, 0, 0);
        assertThat(r.getRequestType()).isEqualTo("image");
        
        r = new ApacheLogRecord(null, null, null, null, null, "/sdfdsf/dfsdfdf/df.mpg", null, 0, 0);
        assertThat(r.getRequestType()).isEqualTo("video");
        
        r = new ApacheLogRecord(null, null, null, null, null, "/sdfdsf/dfsdfdf/df.htm?efw23=235df.jpg", null, 0, 0);
        assertThat(r.getRequestType()).isEqualTo("html");
        
        r = new ApacheLogRecord(null, null, null, null, null, "/sdfdsf/dfsdfdf/df?dfdf", null, 0, 0);
        assertThat(r.getRequestType()).isEqualTo("html");
        
        r = new ApacheLogRecord(null, null, null, null, null, "/sdfdsf/dfsdfdf/df", null, 0, 0);
        assertThat(r.getRequestType()).isEqualTo("html");
     
        r = new ApacheLogRecord(null, null, null, null, null, "/sdfdsf/dfsdfdf/df.pl?TISP", null, 0, 0);
        assertThat(r.getRequestType()).isEqualTo("script");
        
    }
    
}
