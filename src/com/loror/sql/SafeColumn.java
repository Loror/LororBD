package com.loror.sql;

public class SafeColumn {

    private final Object value;

    private SafeColumn(Object value) {
        this.value = value;
    }

    public static SafeColumn of(Object value) {
        return value == null ? null : new SafeColumn(value);
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
