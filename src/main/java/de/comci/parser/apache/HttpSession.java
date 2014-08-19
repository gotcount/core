/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.parser.apache;

import com.google.common.collect.Multiset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeSet;

/**
 *
 * @author Sebastian
 */
public class HttpSession {
    
    private static final SimpleDateFormat fid = new SimpleDateFormat("yyyyMMdd");
    
    private final String client;
    private final Date start;
    private final String id;
    private final TreeSet<ExtendedApacheLogRecord> records = new TreeSet<>();
    
    private Date last;

    public HttpSession(ExtendedApacheLogRecord record, Multiset<String> sessionIds) {
        
        this.client = record.client;
        this.start = record.date;
        this.last = record.date;
        
        // generate id
        String fixedId = fid.format(this.start);
        this.id = fixedId + "-" + (sessionIds.count(fixedId) + 1);
        sessionIds.add(fixedId);
        
        record.sessionState = ExtendedApacheLogRecord.SessionState.START;
        record.sessionId = this.id;
        this.records.add(record);
        
    }
    
    public boolean addRecord(ExtendedApacheLogRecord record, int timeout) {
        if (record.date.getTime() - last.getTime() < timeout * 1000) {
            record.sessionState = ExtendedApacheLogRecord.SessionState.DURING;
            record.sessionId = id;
            this.records.add(record);        
            this.last = record.date;
            return true;
        } else {
            // timeout expired, close last
            records.last().sessionState = ExtendedApacheLogRecord.SessionState.END;
        }
        return false;
    }
    
    public void close() {
        final ExtendedApacheLogRecord last = records.last();
        if (last.sessionState == ExtendedApacheLogRecord.SessionState.START) {
            last.sessionState = ExtendedApacheLogRecord.SessionState.SINGLE;
        } else {
            last.sessionState = ExtendedApacheLogRecord.SessionState.END;
        }
    }
    
}
