/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.cubetree;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.time.DateUtils;

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
     * String -> precision > 0 ? val.substr(0,precision) : val.substr(val.length
     * - precision)
     *
     * Date -> 1000*x : sec, 1000*60*x : min, 1000*60*60*x : hour,
     * 1000*60*60*24*x : day -> precision = 0 ? full
     */
    Double precision = null;
    Double[] buckets;
    boolean bucketsLessThen;

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

    public Dimension precision(int precision) {
        return precision(1.0 * precision);
    }

    public Dimension precision(double precision) {
        if (!Number.class.isAssignableFrom(clasz)
                && !String.class.isAssignableFrom(clasz)
                && !Date.class.isAssignableFrom(clasz)) {
            throw new IllegalArgumentException();
        }
        this.precision = precision;
        return this;
    }

    public Double getPrecision() {
        return precision;
    }

    public Object map(Object value) {

        if (precision == null && buckets == null) {
            return value;
        }

        if (value instanceof Double) {
            return map((Double) value);
        } else if (value instanceof Integer) {
            return map(1l * (Integer)value);
        } else if (value instanceof Long) {
            return map((Long) value);
        } else if (value instanceof String) {
            return map((String) value);
        } else if (value instanceof Date) {
            return map((Date) value);
        }
        throw new UnsupportedOperationException(String.format("value class '%s' is not supported", value.getClass()));
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
        if (precision == null || value == null) {
            return value;
        }
        if (precision > 0) {
            return value.substring(0, Math.min(value.length(), precision.intValue()));
        } else if (precision < 0) {
            return value.substring(value.length() - Math.min(value.length(), -precision.intValue()));
        }
        return value;
    }

    public Date map(Date date) {
        if (precision != null) {
            return DateUtils.truncate(date, precision.intValue());
        }
        return date;
    }

}
