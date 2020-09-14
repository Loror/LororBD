package com.loror.sql;

public class ColumnFilter {

    /**
     * 获取Column
     */
    public static String getColumn(String name, Object var, ModelInfo.ColumnInfo column) {
        if (var != null) {
            return String.valueOf(var);
        } else {
            String defaultValue = column.getDefaultValue();
            if (column.isNotNull() && defaultValue == null) {
                throw new NullPointerException("column " + name + " can not be null");
            }
            return defaultValue;
        }
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
