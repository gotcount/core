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
public class ApacheLogRecord<R extends ApacheLogRecord> implements Comparable<R> {
    
    public final String client;
    public final String clientIdentity;
    public final String remoteUser;
    public final Date date;
    public final String method;
    public final String request;
    public final String protocol;
    public final short statusCode;
    public final int bytesSent;
    public final String referer;
    public final String userAgent;

    private String ifEmpty(String in, String ifEmpty) {
        if (in == null || in.isEmpty() || in.equals("-")) {
            return ifEmpty;
        }
        return in;
    }

    public ApacheLogRecord(String client, String clientIdentity, String remoteUser, Date date, String method, String request, String protocol, short statusCode, int bytesSent) {
        this(client, clientIdentity, remoteUser, date, method, request, protocol, statusCode, bytesSent, null, null);
    }

    public ApacheLogRecord(String client, String clientIdentity, String remoteUser, Date date, String method, String request, String protocol, short statusCode, int bytesSent, String referer, String userAgent) {
        this.client = client;
        this.clientIdentity = ifEmpty(clientIdentity, null);
        this.remoteUser = ifEmpty(remoteUser, null);
        this.date = date;
        this.method = method;
        this.request = request;
        this.protocol = protocol;
        this.statusCode = statusCode;
        this.bytesSent = bytesSent;
        this.referer = referer;
        this.userAgent = userAgent;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.client);
        hash = 29 * hash + Objects.hashCode(this.clientIdentity);
        hash = 29 * hash + Objects.hashCode(this.remoteUser);
        hash = 29 * hash + Objects.hashCode(this.date);
        hash = 29 * hash + Objects.hashCode(this.method);
        hash = 29 * hash + Objects.hashCode(this.request);
        hash = 29 * hash + Objects.hashCode(this.protocol);
        hash = 29 * hash + this.statusCode;
        hash = 29 * hash + (int) (this.bytesSent ^ (this.bytesSent >>> 32));
        hash = 29 * hash + Objects.hashCode(this.referer);
        hash = 29 * hash + Objects.hashCode(this.userAgent);
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
        final ApacheLogRecord other = (ApacheLogRecord) obj;
        if (!Objects.equals(this.client, other.client)) {
            return false;
        }
        if (!Objects.equals(this.clientIdentity, other.clientIdentity)) {
            return false;
        }
        if (!Objects.equals(this.remoteUser, other.remoteUser)) {
            return false;
        }
        if (!Objects.equals(this.date, other.date)) {
            return false;
        }
        if (!Objects.equals(this.method, other.method)) {
            return false;
        }
        if (!Objects.equals(this.request, other.request)) {
            return false;
        }
        if (!Objects.equals(this.protocol, other.protocol)) {
            return false;
        }
        if (this.statusCode != other.statusCode) {
            return false;
        }
        if (this.bytesSent != other.bytesSent) {
            return false;
        }
        if (!Objects.equals(this.referer, other.referer)) {
            return false;
        }
        if (!Objects.equals(this.userAgent, other.userAgent)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ApacheLogRecord{" + "client=" + client + ", clientIdentity=" + clientIdentity + ", remoteUser=" + remoteUser + ", date=" + date + ", method=" + method + ", request=" + request + ", protocol=" + protocol + ", statusCode=" + statusCode + ", bytesSent=" + bytesSent + ", referer=" + referer + ", userAgent=" + userAgent + "}";
    }

    @Override
    public int compareTo(R o) {
        return this.date.compareTo(o.date);
    }

}
