package com.loror.sql.mysql;

import com.loror.sql.*;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MySQLClient implements SQLClient {

    private String url, name, password;
    private MySQLDataBase mySQLDataBase;
    private List<Class<?>> execTableCreated = new LinkedList<>();//已执行过创建
    private List<Class<?>> execTableUpdated = new LinkedList<>();//已执行过更新表
    private HashMap<Class<?>, ModelInfo> classModel = new HashMap<>();
    private OnClose onClose;//执行close方法时候调用，如果为空则执行关闭连接

    public MySQLClient(String url, String name, String password) {
        this.url = url;
        this.name = name;
        this.password = password;
        init(url, name, password);
    }

    /**
     * 初始化
     */
    private void init(String url, String name, String password) {
        close();
        try {
            mySQLDataBase = new MySQLDataBase("com.mysql.jdbc.Driver", url, name, password);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public MySQLDataBase getDatabase() {
        return mySQLDataBase;
    }

    @Override
    public void setOnClose(OnClose onClose) {
        this.onClose = onClose;
    }

    @Override
    public void reStart() {
        if (this.mySQLDataBase == null) {
            init(url, name, password);
        }
    }

    @Override
    public void close() {
        if (this.onClose != null) {
            this.onClose.close(this);
        } else {
            if (this.mySQLDataBase != null) {
                try {
                    this.mySQLDataBase.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                this.mySQLDataBase = null;
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.mySQLDataBase == null;
    }

    @Override
    public ModelInfo getModel(Class<?> table) {
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

    @Override
    public void createTableIfNotExists(Class<?> table) {
        if (execTableCreated.contains(table)) {
            return;
        }
        execTableCreated.add(table);
        try {
            mySQLDataBase.getPst(MySQLBuilder.getCreateSql(getModel(table)), false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dropTable(Class<?> table) {
        try {
            mySQLDataBase.getPst(MySQLBuilder.getDropTableSql(getModel(table)), false, pst -> {
                pst.execute();
            });
            execTableCreated.remove(table);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void changeTableIfColumnAdd(Class<?> table) {
        if (execTableUpdated.contains(table)) {
            return;
        }
        execTableUpdated.add(table);
        try {
            ModelInfo modelInfo = getModel(table);
            mySQLDataBase.getPst("select * from " + modelInfo.getSafeTableName() + " limit 1", false, preparedStatement -> {
                ResultSet cursor = preparedStatement.executeQuery();
                List<String> columnNames = new ArrayList<>();
                ResultSetMetaData data = cursor.getMetaData();
                for (int i = 0; i < data.getColumnCount(); i++) {
                    columnNames.add(data.getColumnName(i + 1));
                }
                cursor.close();
                String idName = null;
                List<String> newColumnNames = new ArrayList<>();
                HashMap<String, ModelInfo.ColumnInfo> columnHashMap = new HashMap<>();
                for (ModelInfo.ColumnInfo columnInfo : modelInfo.getColumnInfos()) {
                    if (columnInfo.isPrimaryKey()) {
                        idName = columnInfo.getName();
                    } else {
                        newColumnNames.add(columnInfo.getName());
                        columnHashMap.put(columnInfo.getName(), columnInfo);
                    }
                }

                int i;
                List<String> different = new ArrayList<>();
                for (i = 0; i < columnNames.size(); i++) {
                    String columnName = columnNames.get(i);
                    if (idName != null && idName.equals(columnName)) {
                        continue;
                    }
                    boolean remove = newColumnNames.remove(columnName);
                    if (!remove) {
                        different.add(columnName);
                    }
                }
                if (different.size() > 0) {
                    StringBuilder builder = new StringBuilder("[");
                    for (String var : different) {
                        builder.append(var)
                                .append(",");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append("]");
                    throw new IllegalStateException("cannot reduce column at this function:" + builder.toString());
                }
                for (i = 0; i < newColumnNames.size(); i++) {
                    String newColumnName = newColumnNames.get(i);
                    ModelInfo.ColumnInfo columnInfo = columnHashMap.get(newColumnName);
                    String type = columnInfo.getType();
                    String defaultValue = columnInfo.getDefaultValue();
                    mySQLDataBase.getPst("alter table " + modelInfo.getSafeTableName() + " add column `" + newColumnName + "` " + type, false, new MySQLDataBase.OnGetPst() {
                        @Override
                        public void getPst(PreparedStatement pst) throws SQLException {
                            pst.execute();
                            if (defaultValue != null && defaultValue.length() > 0) {
                                pst.execute("update " + modelInfo.getSafeTableName() + " set `" + newColumnName +
                                        "` = '" + ColumnFilter.safeColumn(defaultValue) + "'");
                            }
                        }
                    });
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T> Model<T> model(Class<T> table) {
        ModelInfo model = getModel(table);
        if (model.isCheckTable()) {
            createTableIfNotExists(table);
            try {
                changeTableIfColumnAdd(table);
            } catch (Exception e) {
                System.err.println("SQLiteUtil:" + "changeTable failed:" + e);
            }
        }
        return new MySQLModel<>(table, this, getModel(table));
    }

    @Override
    public <T> Model<T> model(Class<T> table, boolean checkTable) {
        if (checkTable) {
            createTableIfNotExists(table);
            try {
                changeTableIfColumnAdd(table);
            } catch (Exception e) {
                System.err.println("SQLiteUtil:" + "changeTable failed:" + e);
            }
        }
        return new MySQLModel<>(table, this, getModel(table));
    }

    @Override
    public boolean transaction(Runnable runnable) {
        if (runnable == null) {
            return false;
        }

        Connection connection = mySQLDataBase.getConn();
        if (connection == null) {
            return false;
        }

        try {
            if (!connection.getAutoCommit()) {
                runnable.run();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            connection.setAutoCommit(false);
            try {
                runnable.run();
                connection.commit();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 插入
     */
    protected void insert(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            ModelInfo modelInfo = getModel(entity.getClass());
            ModelInfo.ColumnInfo id = modelInfo.getId();
            boolean returnId = id != null && id.isReturnKey();
            mySQLDataBase.getPst(MySQLBuilder.getInsertSql(entity, modelInfo), returnId, pst -> {
                pst.execute();
                if (returnId) {
                    // 在执行更新后获取自增长列
                    ResultSet resultSet = pst.getGeneratedKeys();
                    int num = 0;
                    if (resultSet.next()) {
                        num = resultSet.getInt(1);
                        resultSet.close();
                    }
                    if (num != 0) {
                        Class<?> type = id.getTypeClass();
                        Field field = id.getField();
                        if (type == int.class || type == Integer.class) {
                            field.setAccessible(true);
                            try {
                                field.set(entity, num);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        } else if (type == long.class || type == Long.class) {
                            field.setAccessible(true);
                            try {
                                field.set(entity, (long) num);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据id更新数据
     */
    protected void updateById(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            mySQLDataBase.getPst(MySQLBuilder.getUpdateSql(entity, getModel(entity.getClass())), false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除数据
     */
    protected void delete(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            mySQLDataBase.getPst(MySQLBuilder.getDeleteSql(entity, getModel(entity.getClass())), false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除所有数据
     */
    protected void deleteAll(Class<?> table) {
        try {
            mySQLDataBase.getPst("delete from " + getModel(table).getSafeTableName(), false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * id删除
     */
    protected void deleteById(String id, Class<?> table) {
        try {
            mySQLDataBase.getPst("delete from " + getModel(table).getSafeTableName() + " where " + getModel(table).getId().getSafeName() + " = '" + id + "'", false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取所有数据
     */
    protected <T> List<T> getAll(Class<T> table) {
        List<T> entitys = new ArrayList<>();
        try {
            mySQLDataBase.getPst("select * from " + getModel(table).getSafeTableName(), false, pst -> {
                ResultSet cursor = pst.executeQuery();
                List<ModelResult> modelResults = MySQLResult.find(cursor);
                cursor.close();
                ModelInfo modelInfo = getModel(table);
                for (ModelResult modelResult : modelResults) {
                    entitys.add(modelResult.toObject(modelInfo));
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entitys;
    }

    /**
     * 获取条目
     */
    protected int count(Class<?> table) {
        int[] count = {0};
        try {
            mySQLDataBase.getPst("select count(1) from " + getModel(table).getSafeTableName(), false, pst -> {
                ResultSet cursor = pst.executeQuery();
                if (cursor.next()) {
                    try {
                        count[0] = cursor.getInt(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                cursor.close();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count[0];
    }
}
