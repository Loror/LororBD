package com.loror.sql;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ModelInfo {

    private Class<?> tableClass;
    private String tableName;
    private boolean checkTable;
    private List<ColumnInfo> columnInfos = new ArrayList<>();

    public ModelInfo(Class<?> tableClass) {
        this.tableClass = tableClass;
        findTable(tableClass);
        findColumns(tableClass);
    }

    private void findTable(Class<?> tableClass) {
        Table table = (Table) tableClass.getAnnotation(Table.class);
        if (table == null) {
            throw new IllegalArgumentException("this class does not define table");
        }
        tableName = table.name();
        checkTable = table.checkTable();
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
                columnInfos.add(new ColumnInfo(false, false, field, columnName));
            } else {
                Id id = (Id) field.getAnnotation(Id.class);
                if (id != null) {
                    if (findPrimaryKey) {
                        throw new IllegalArgumentException("table cannot contain more than 1 primary key");
                    }
                    findPrimaryKey = true;
                    String idName = id.name();
                    columnInfos.add(new ColumnInfo(true, id.returnKey(), field, idName.length() == 0 ? "id" : idName));
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

    public boolean isCheckTable() {
        return checkTable;
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
        private Field field;

        public ColumnInfo(boolean primaryKey, boolean returnKey, Field field, String name) {
            this.primaryKey = primaryKey;
            this.returnKey = returnKey;
            this.field = field;
            this.typeClass = field.getType();
            this.name = name;
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
            if (typeClass == int.class || typeClass == long.class || typeClass == Integer.class || typeClass == Long.class) {
                type = "int";
            } else if (typeClass == float.class || typeClass == Float.class) {
                type = "float";
            } else if (typeClass == double.class || typeClass == Double.class) {
                type = "double";
            } else if (typeClass == String.class) {
                type = "varchar(255)";
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

        public String getType() {
            return type;
        }

    }
}
