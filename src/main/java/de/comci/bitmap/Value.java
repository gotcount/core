/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

import java.util.Objects;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 * @param <T>
 */
public class Value<T> implements Comparable<Value<T>> {

    private final T value;
    private final Class<T> type;
    
    public static <T> Value<T> get(T value) {
        if (value == null) {
            throw new IllegalArgumentException("null not allowed");
        }
        return new Value(value, value.getClass());
    }
    
    public static <T> Value<T> empty(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("null not allowed");
        }
        return new Value(null, type);
    }

    public Value(T value, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (value != null && value.getClass() != type) {
            throw new IllegalArgumentException("types do not match");
        }
        this.value = value;
        this.type = type;
    }

    public T getValue() {
        return value;
    }

    public Class<T> getType() {
        return type;
    }
    
    public String getLabel() {        
        return (value != null) ? value.toString() : "null";
    }

    @Override
    public int compareTo(Value<T> o) {
        int c = 0;
        if (o != null) {
            if (this.type != o.type) {
                throw new IllegalArgumentException("cannot compare values of different type");
            }
            if (this.value != null && o.value != null) {                
                if (Comparable.class.isAssignableFrom(this.type)) {
                    c = ((Comparable) this.value).compareTo(o.value);
                }
            } else {
                c = (this.value == null) ? 1 : -1; // null first
            }
        }
        return c;
    }

    @Override
    public int hashCode() {
        return 71 * 5 + Objects.hashCode(this.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Value<?> other = (Value<?>) obj;
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("<%s:%s>", type.getSimpleName(), (value == null) ? "null" : value.toString());
    }
    
}
