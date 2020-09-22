package com.loror.sql;

public class UnChangeValue {

    private final Object value;

    private UnChangeValue(Object value) {
        this.value = value;
    }

    public static UnChangeValue of(Object value) {
        return value == null ? null : new UnChangeValue(value);
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
