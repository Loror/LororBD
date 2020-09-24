package com.loror.sql;

public class ColumnFilter {

    /**
     * 获取value
     */
    public static String getValue(Object var, ModelInfo.ColumnInfo column) {
        if (var != null) {
            return String.valueOf(var);
        } else {
            if (column != null) {
                String defaultValue = column.getDefaultValue();
                if (column.isNotNull() && defaultValue == null) {
                    throw new NullPointerException("column " + column.getName() + " can not be null");
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
    public static String safeValue(Object var) {
        if (var == null) {
            return null;
        }
        if (var instanceof Number || var instanceof UnChangeValue) {
            return var.toString();
        }
        return "'" + var.toString().replace("'", "''") + "'";
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
