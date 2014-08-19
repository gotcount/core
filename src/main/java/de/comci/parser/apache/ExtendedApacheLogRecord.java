/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.parser.apache;

import java.util.Date;
import java.util.Objects;

/**
 *
 * @author Sebastian
 */
public class ExtendedApacheLogRecord extends ApacheLogRecord {

    String sessionId;
    SessionState sessionState;
    String countryCode;
    
    public ExtendedApacheLogRecord(ApacheLogRecord record) {
        super(record.client, record.clientIdentity, record.remoteUser, record.date, record.method, record.request, record.protocol, record.statusCode, record.bytesSent, record.referer, record.userAgent);
    }

    public ExtendedApacheLogRecord(String client, String clientIdentity, String remoteUser, Date date, String method, String request, String protocol, short statusCode, int bytesSent, String referer, String userAgent, String sessionId, SessionState sessionState) {
        super(client, clientIdentity, remoteUser, date, method, request, protocol, statusCode, bytesSent, referer, userAgent);
        this.sessionId = sessionId;
        this.sessionState = sessionState;
    }
    
    public enum SessionState {
        START, END, DURING, SINGLE
    }

    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ExtendedApacheLogRecord other = (ExtendedApacheLogRecord) obj;
        if (!Objects.equals(this.sessionId, other.sessionId)) {
            return false;
        }
        if (this.sessionState != other.sessionState) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ExtendedApacheLogRecord{" + "client=" + client + ", clientIdentity=" + clientIdentity + ", remoteUser=" + remoteUser + ", date=" + date + ", method=" + method + ", request=" + request + ", protocol=" + protocol + ", statusCode=" + statusCode + ", bytesSent=" + bytesSent + ", referer=" + referer + ", userAgent=" + userAgent + ", sessionId=" + sessionId + ", sessionState=" + sessionState + '}';
    }

}
