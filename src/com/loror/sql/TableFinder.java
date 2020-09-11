package com.loror.sql;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.HashMap;

public class TableFinder {

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
                        .append(" int NOT NULL AUTO_INCREMENT,");
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

        StringBuilder builder = new StringBuilder();
        builder.append(getUpdateSqlNoWhere(entity, modelInfo, false));
        builder.append(" where ")
                .append(idName)
                .append(" = ")
                .append(idVolume);
        return builder.toString();
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
                    Column column = (Column) field.getAnnotation(Column.class);
                    columns.put(columnInfo.getSafeName(), ColumnFilter.getColumn(columnInfo.getName(), object, column));
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
                    Column column = (Column) field.getAnnotation(Column.class);
                    columns.put(columnInfo.getSafeName(), ColumnFilter.getColumn(columnInfo.getName(), object, column));
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
                    Column column = (Column) field.getAnnotation(Column.class);
                    columns.put(columnInfo.getSafeName(), ColumnFilter.getColumn(columnInfo.getName(), object, column));
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

    /**
     * 查询sql数据到对象中
     */
    public static void find(Object entity, ResultSet cursor) {
        Class<?> handlerType = entity.getClass();
        Field[] fields = handlerType.getDeclaredFields();
        if (fields == null || fields.length == 0) {
            return;
        }
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            try {
                Column column = (Column) field.getAnnotation(Column.class);
                if (column != null) {
                    String columnName = column.name();
                    if (columnName.length() == 0) {
                        columnName = field.getName();
                    }
                    Class<?> type = field.getType();
                    int index;
                    try {
                        index = cursor.findColumn(columnName);
                    } catch (Exception e) {
                        index = -1;
                    }
                    if (index <= 0) {
                        continue;
                    }
                    String result = cursor.getString(index);
                    result = ColumnFilter.decodeColumn(result, column);
                    if (result != null) {
                        if (type == int.class || type == Integer.class) {
                            field.set(entity, Integer.parseInt(result));
                        } else if (type == long.class || type == Long.class) {
                            field.set(entity, Long.parseLong(result));
                        } else if (type == float.class || type == Float.class) {
                            field.set(entity, Float.parseFloat(result));
                        } else if (type == double.class || type == Double.class) {
                            field.set(entity, Double.parseDouble(result));
                        } else if (type == String.class) {
                            field.set(entity, result);
                        }
                    }
                } else {
                    Id id = (Id) field.getAnnotation(Id.class);
                    if (id != null) {
                        String name = id.name();
                        if (name.length() == 0) {
                            name = "id";
                        }
                        int index;
                        try {
                            index = cursor.findColumn(name);
                        } catch (Exception e) {
                            index = -1;
                        }
                        if (index <= 0) {
                            continue;
                        }
                        String result = cursor.getString(index);
                        Class<?> type = field.getType();
                        if (type == int.class || type == Integer.class) {
                            field.set(entity, Integer.parseInt(result));
                        } else {
                            field.set(entity, Long.parseLong(result));
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
