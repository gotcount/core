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
    private final String label;

    /**
     * Get a value object from the given object. The value type will be derived 
     * from the given object.
     * 
     * @param <T>
     * @param value
     * @return 
     */
    public static <T> Value<T> get(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return new Value(value, value.getClass());
    }

    /**
     * Construct an value object with a null value of the given type
     * @param <T>
     * @param type
     * @return 
     */
    public static <T> Value<T> empty(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must be supplied");
        }
        return new Value(null, type);
    }
    
    public static <T> Value<T> bucket(Class<T> type, String label) {
        if (type == null) {
            throw new IllegalArgumentException("type must be supplied");
        }
        if (label == null) {
            throw new IllegalArgumentException("label must be supplied");
        }
        return new Value<>(null, type, label);
    }
    
    private Value(T value, Class<T> type, String label) {
        
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        
        if (value != null && !type.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format("type is not assignable (%s != %s)", value.getClass(), type));
        }

        this.value = value;
        this.type = type;
        this.label = label;        
        
    }
    
    public Value(T value, Class<T> type) {
        this(value, type, (value != null) ? value.toString() : "");
    }

    public T getValue() {
        return value;
    }

    public Class<T> getType() {
        return type;
    }

    public String getLabel() {
        return label;
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
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.value);
        hash = 11 * hash + Objects.hashCode(this.type);
        hash = 11 * hash + Objects.hashCode(this.label);
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
        final Value<?> other = (Value<?>) obj;
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.label, other.label)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("<%s:%s>", getLabel(), type.getSimpleName());
    }

}
