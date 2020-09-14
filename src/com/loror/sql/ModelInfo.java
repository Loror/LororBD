package com.loror.sql;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ModelInfo {

    private Class<?> tableClass;
    private String tableName;
    private List<ColumnInfo> columnInfos = new ArrayList<>();

    private static HashMap<Class<?>, ModelInfo> classModel = new HashMap<>();

    private ModelInfo(Class<?> tableClass) {
        this.tableClass = tableClass;
        findTable(tableClass);
        findColumns(tableClass);
    }

    public static ModelInfo of(Class<?> table) {
        if (table == null) {
            return null;
        }
        ModelInfo model = classModel.get(table);
        if (model == null) {
            model = new ModelInfo(table);
            classModel.put(table, model);
        }
        return model;
    }

    private void findTable(Class<?> tableClass) {
        Table table = (Table) tableClass.getAnnotation(Table.class);
        if (table == null) {
            throw new IllegalArgumentException("this class does not define table");
        }
        tableName = table.name();
        if (tableName.length() == 0) {
            tableName = tableClass.getSimpleName();
        }
    }

    private void findColumns(Class<?> tableClass) {
        columnInfos.clear();
        Field[] fields = tableClass.getDeclaredFields();
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("this table does not contains any field");
        }
        boolean findPrimaryKey = false;
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            Column column = (Column) field.getAnnotation(Column.class);
            if (column != null) {
                String columnName = column.name();
                if (columnName.length() == 0) {
                    columnName = field.getName();
                }
                columnInfos.add(new ColumnInfo(false, false, field,
                        columnName, column.length(), column.scale(), column.defaultValue(), column.notNull()));
            } else {
                Id id = (Id) field.getAnnotation(Id.class);
                if (id != null) {
                    if (findPrimaryKey) {
                        throw new IllegalArgumentException("table cannot contain more than 1 primary key");
                    }
                    findPrimaryKey = true;
                    String idName = id.name();
                    columnInfos.add(new ColumnInfo(true, id.returnKey(), field,
                            idName.length() == 0 ? "id" : idName, id.length(), 0, null, true));
                }
            }
        }
        if (columnInfos.size() == 0) {
            throw new IllegalArgumentException("this table does not contains any column");
        }
    }

    public Class<?> getTableClass() {
        return tableClass;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSafeTableName() {
        return "`" + tableName + "`";
    }

    public List<ColumnInfo> getColumnInfos() {
        return columnInfos;
    }

    public ColumnInfo getId() {
        ColumnInfo idColumn = null;
        for (ColumnInfo columnInfo : columnInfos) {
            if (columnInfo.isPrimaryKey()) {
                idColumn = columnInfo;
                break;
            }
        }
        return idColumn;
    }

    public Object getTableObject() throws Exception {
        try {
            return tableClass.newInstance();
        } catch (Exception e) {
            Constructor<?> constructor = tableClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
    }

    public static class ColumnInfo {

        private boolean primaryKey;
        private boolean returnKey;
        private Class<?> typeClass;
        private String type;
        private String name;
        private int length;
        private int scale;
        private String defaultValue;
        private boolean notNull;
        private Field field;

        public ColumnInfo(boolean primaryKey, boolean returnKey, Field field, String name, int length, int scale, String defaultValue, boolean notNull) {
            this.primaryKey = primaryKey;
            this.returnKey = returnKey;
            this.field = field;
            this.typeClass = field.getType();
            this.name = name;
            this.length = length;
            this.scale = scale;
            this.defaultValue = "".equals(defaultValue) ? null : defaultValue;
            this.notNull = notNull;
            if (primaryKey) {
                if (typeClass != int.class && typeClass != long.class &&
                        typeClass != Integer.class && typeClass != Long.class) {
                    throw new IllegalArgumentException("primary key must be Integer or Long :" + name);
                }
            }
            this.type = typeByClass();
            if (this.type == null) {
                throw new IllegalArgumentException("unsupported column type " + typeClass.getSimpleName() + " :" + name);
            }
        }

        private String typeByClass() {
            String type = null;
            String max = length == 0 ? "" : ("(" + length + ")");
            if (typeClass == int.class || typeClass == long.class || typeClass == Integer.class || typeClass == Long.class) {
                type = "int" + (max.length() == 0 ? "" : max);
            } else if (typeClass == float.class || typeClass == Float.class) {
                type = "float" + (max.length() == 0 ? "" : max.replace(")", ("," + scale + ")")));
            } else if (typeClass == double.class || typeClass == Double.class) {
                type = "double" + (max.length() == 0 ? "" : max.replace(")", ("," + scale + ")")));
            } else if (typeClass == String.class) {
                type = "varchar" + (max.length() == 0 ? "(255)" : max);
            }
            return type;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        public boolean isReturnKey() {
            return returnKey;
        }

        public Field getField() {
            return field;
        }

        public Class<?> getTypeClass() {
            return typeClass;
        }

        public String getName() {
            return name;
        }

        public String getSafeName() {
            return "`" + name + "`";
        }

        public int getLength() {
            return length;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public boolean isNotNull() {
            return notNull;
        }

        public String getType() {
            return type;
        }

    }
}
