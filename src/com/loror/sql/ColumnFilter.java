package com.loror.sql;

public class ColumnFilter {

    /**
     * 获取Column
     */
    public static String getColumn(String name, Object var, ModelInfo.ColumnInfo column) {
        if (var != null) {
            return String.valueOf(var);
        } else {
            if (column != null) {
                String defaultValue = column.getDefaultValue();
                if (column.isNotNull() && defaultValue == null) {
                    throw new NullPointerException("column " + name + " can not be null");
                }
                return defaultValue;
            } else {
                return null;
            }
        }
    }

    /**
     * 安全处理
     */
    public static String safeColumn(Object column) {
        if (column == null) {
            return null;
        }
        if (column instanceof Number) {
            return column.toString();
        }
        if (column instanceof SafeColumn) {
            return column.toString();
        }
        return "'" + column.toString().replace("'", "''") + "'";
    }

    /**
     * 是否不需要添加`
     */
    public static boolean isFullName(String name) {
        return name.contains("(")
                || name.contains(",")
                || name.contains("`")
                || name.contains("'")
                || name.contains(".")
                || name.contains(" as ");
    }
}
