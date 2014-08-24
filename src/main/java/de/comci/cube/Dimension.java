/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.comci.cube;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Sebastian
 */
public class Dimension {
    
    final String name;
    final Class<?> clasz;
    /**
     * int,long -> (Math.floor(val / precision)) * precision 
     * 
     * float,double -> (Math.floor(val / precision)) * precision 
     * 
     * String -> precision > 0 ? val.substr(0,precision) : val.substr(val.length - precision) 
     * 
     * Date -> 1000*x : sec, 1000*60*x : min, 1000*60*60*x : hour, 1000*60*60*24*x : day 
     * -> precision = 0 ? full
     */
    private Double precision = null;
    private TimeUnit timePrecision = TimeUnit.DAY;
    private Double[] buckets;
    private boolean bucketsLessThen;

    public Dimension(String name) {
        this(name, Object.class);
    }

    public Dimension(String name, Class<?> clasz) {
        this.name = name;
        this.clasz = clasz;
    }

    public boolean isComparable() {
        return Comparable.class.isAssignableFrom(this.clasz);
    }

    @Override
    public String toString() {
        return String.format("Dimension:%s:%s", name, clasz.getName());
    }
    
    public Dimension buckets(Double... buckets) {
        return buckets(true, buckets);
    }
    
    public Dimension buckets(boolean lessThen, Double... buckets) {
        if (!Number.class.isAssignableFrom(clasz)) {
            throw new IllegalArgumentException();
        }
        Arrays.sort(buckets); // ensure order
        this.buckets = buckets;
        this.bucketsLessThen = lessThen;
        return this;
    }
    
    public Dimension precision(double precision) {
        if (!Number.class.isAssignableFrom(clasz) && !String.class.isAssignableFrom(clasz)) {
            throw new IllegalArgumentException();
        }
        this.precision = precision;
        return this;
    }
    
    public Dimension precision(TimeUnit timeUnit) {
        if (!Date.class.isAssignableFrom(clasz)) {
            throw new UnsupportedOperationException();
        }
        timePrecision = timeUnit;
        return this;
    }
        
    public Double map(double value) {
        if (precision != null) {
            return Math.floor(value / precision) * precision;
        } else if (buckets != null) {
            if (bucketsLessThen) {
                for (int i = 0; i < buckets.length; i++) {
                    if (value <= buckets[i]) {
                        return buckets[i];
                    }
                }
            } else {
                for (int i = buckets.length - 1; i > 0; i--) {
                    if (value >= buckets[i]) {
                        return buckets[i];
                    }
                }                
            }
        }
        return value;
    }
    
    public Long map(Long value) {
        return map(value.doubleValue()).longValue();
    }
    
    public String map(String value) {
        if (precision == null || value == null)
            return value;
        if (precision > 0) {
            return value.substring(0, Math.min(value.length(), precision.intValue()));
        } else if (precision < 0) {
            return value.substring(value.length() - Math.min(value.length(), -precision.intValue()));
        }
        return value;
    }
    
    public Time map(Date date) {
        return new Time(date, timePrecision);
    }
    
}
