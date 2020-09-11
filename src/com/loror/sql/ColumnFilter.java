package com.loror.sql;

public class ColumnFilter {

    /**
     * 获取Column
     */
    public static String getColumn(String name, Object var, Column column) {
        if (var != null) {
            String value = String.valueOf(var);
            return String.valueOf(var);
        } else {
            String defaultValue = column.defaultValue();
            if (column.notNull() && defaultValue.length() == 0) {
                throw new NullPointerException("column " + name + " can not be null");
            }
            return defaultValue.length() == 0 ? null : defaultValue;
        }
    }

    /**
     * 获取Column
     */
    public static String decodeColumn(Object var, Column column) {
        if (var != null) {
            return String.valueOf(var);
        }
        return null;
    }

    /**
     * 安全处理
     */
    public static String safeColumn(Object column) {
        if (column == null) {
            return null;
        }
        return column.toString().replace("'", "''");
    }
}
