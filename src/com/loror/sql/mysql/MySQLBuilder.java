package com.loror.sql.mysql;

import com.loror.sql.ColumnFilter;
import com.loror.sql.ModelInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MySQLBuilder {

    /**
     * 获得创建语句
     */
    public static String getCreateSql(ModelInfo modelInfo) {
        StringBuilder builder = new StringBuilder();
        String primary = null;
        builder.append("create table if not exists ")
                .append(modelInfo.getSafeTableName())
                .append("(");
        for (ModelInfo.ColumnInfo columnInfo : modelInfo.getColumnInfos()) {
            if (columnInfo.isPrimaryKey()) {
                builder.append(columnInfo.getSafeName())
                        .append(" ")
                        .append(columnInfo.getType())
                        .append(" NOT NULL AUTO_INCREMENT,");
                primary = columnInfo.getSafeName();
            } else {
                builder.append(columnInfo.getSafeName())
                        .append(" ")
                        .append(columnInfo.getType())
                        .append(",");
            }
        }
        if (primary != null) {
            builder.append("PRIMARY KEY (")
                    .append(primary)
                    .append(")");
        } else {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     * 获得删表语句
     */
    public static String getDropTableSql(ModelInfo modelInfo) {
        return "drop table if exists " + modelInfo.getSafeTableName();
    }

    /**
     * 获取最后自增id语句
     */
    public static String getLastIdSql(ModelInfo modelInfo) {
        return "select last_insert_rowid() from " + modelInfo.getSafeTableName();
    }

    /**
     * 获得更新语句
     */
    public static String getUpdateSql(Object entity, ModelInfo modelInfo) {
        String idName = "id";
        String idVolume = "0";
        ModelInfo.ColumnInfo columnInfo = modelInfo.getId();
        if (columnInfo != null) {
            idName = columnInfo.getSafeName();
            Field field = columnInfo.getField();
            field.setAccessible(true);
            try {
                Object object = field.get(entity);
                if (object != null) {
                    idVolume = object.toString();
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return getUpdateSqlNoWhere(entity, modelInfo, false) +
                " where `" + idName + "` = " + idVolume;
    }

    /**
     * 获得更新语句
     */
    public static String getUpdateSqlNoWhere(Object entity, ModelInfo modelInfo, boolean ignoreNull) {
        HashMap<String, String> columns = new HashMap<>();
        for (ModelInfo.ColumnInfo columnInfo : modelInfo.getColumnInfos()) {
            Field field = columnInfo.getField();
            field.setAccessible(true);
            try {
                if (!columnInfo.isPrimaryKey()) {
                    Object object = field.get(entity);
                    if (ignoreNull && object == null) {
                        continue;
                    }
                    columns.put(columnInfo.getSafeName(), ColumnFilter.getColumn(columnInfo.getName(), object, columnInfo));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("update ")
                .append(modelInfo.getSafeTableName())
                .append(" set ");
        for (String o : columns.keySet()) {
            if (columns.get(o) == null) {
                builder.append(o)
                        .append(" = null,");
            } else {
                builder.append(o)
                        .append(" = '")
                        .append(ColumnFilter.safeColumn(columns.get(o)))
                        .append("',");
            }
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    /**
     * 获得更新语句
     */
    public static String getUpdateSqlNoWhere(Map<String, Object> values, String table) {
        StringBuilder builder = new StringBuilder();
        builder.append("update ")
                .append(table)
                .append(" set ");
        for (String name : values.keySet()) {
            Object value = values.get(name);
            if (!ColumnFilter.isFullName(name)) {
                name = "`" + name + "`";
            }
            if (value == null) {
                builder.append(name)
                        .append(" = null,");
            } else {
                builder.append(name)
                        .append(" = '")
                        .append(ColumnFilter.safeColumn(value))
                        .append("',");
            }
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    /**
     * 获得插入语句
     */
    public static String getInsertSql(Object entity, ModelInfo modelInfo) {
        HashMap<String, String> columns = new HashMap<>();
        for (ModelInfo.ColumnInfo columnInfo : modelInfo.getColumnInfos()) {
            Field field = columnInfo.getField();
            field.setAccessible(true);
            try {
                Object object = field.get(entity);
                if (columnInfo.isPrimaryKey()) {
                    if (object != null) {
                        long idValue = Long.parseLong(object.toString());
                        if (idValue > 0) {
                            columns.put(columnInfo.getSafeName(), object.toString());
                        }
                    }
                } else {
                    columns.put(columnInfo.getSafeName(), ColumnFilter.getColumn(columnInfo.getName(), object, columnInfo));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        StringBuilder keys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (String o : columns.keySet()) {
            keys.append(o)
                    .append(",");
            if (columns.get(o) == null) {
                values.append("null,");
            } else {
                values.append("'")
                        .append(ColumnFilter.safeColumn(columns.get(o)))
                        .append("',");
            }
        }
        keys.deleteCharAt(keys.length() - 1);
        values.deleteCharAt(values.length() - 1);
        return "insert into " + modelInfo.getSafeTableName() + "(" + keys.toString() + ")" + " values " + "(" + values.toString() + ")";
    }

    /**
     * 获得删除语句
     */
    public static String getDeleteSql(Object entity, ModelInfo modelInfo) {
        HashMap<String, String> columns = new HashMap<>();
        for (ModelInfo.ColumnInfo columnInfo : modelInfo.getColumnInfos()) {
            Field field = columnInfo.getField();
            field.setAccessible(true);
            try {
                Object object = field.get(entity);
                if (columnInfo.isPrimaryKey()) {
                    columns.put(columnInfo.getSafeName(), String.valueOf(object));
                } else {
                    columns.put(columnInfo.getSafeName(), ColumnFilter.getColumn(columnInfo.getName(), object, columnInfo));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("delete from ")
                .append(modelInfo.getSafeTableName())
                .append(" where ");
        for (String o : columns.keySet()) {
            if (columns.get(o) == null) {
                builder.append(o)
                        .append(" is null and ");
            } else {
                builder.append(o)
                        .append(" = '")
                        .append(ColumnFilter.safeColumn(columns.get(o)))
                        .append("' and ");
            }
        }
        return builder.toString().substring(0, builder.toString().length() - 5);
    }

}
