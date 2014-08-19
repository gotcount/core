/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.parser.apache;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Sebastian
 */
public class ApacheSessionParser {

    private final Multiset<String> sessionIds = HashMultiset.create();
    private final Map<String, HttpSession> currentSessions = new HashMap<>();
    private final Set<HttpSession> allSessions = new HashSet<>();
    private final int timeout;

    public ApacheSessionParser(int timeout) {
        this.timeout = timeout;
    }
    
    public ExtendedApacheLogRecord parse(ApacheLogRecord record) {
        
        ExtendedApacheLogRecord er = new ExtendedApacheLogRecord(record);
        
        HttpSession session;
        String client = er.client;

        if (!currentSessions.containsKey(client) || !currentSessions.get(client).addRecord(er, timeout)) {
            session = new HttpSession(er, sessionIds);
            currentSessions.put(client, session);
            allSessions.add(session);
        }        
        return er;
        
    }
    
    public void done() {
        // close all open sessions
        allSessions.forEach(HttpSession::close);
    }

}
