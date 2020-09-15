package com.loror.sql.mysql;

import com.loror.sql.*;

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
    private OnClose onClose;//执行close方法时候调用，如果为空则执行关闭连接
    private LogListener logListener;

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
            mySQLDataBase = new MySQLDataBase("com.mysql.jdbc.Driver", url, name, password) {
                @Override
                public void onGetPst(String sql) {
                    if (logListener != null) {
                        logListener.log(sql);
                    }
                }
            };
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
    public void setLogListener(LogListener logListener) {
        this.logListener = logListener;
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
            this.onClose.close(mySQLDataBase);
        } else {
            if (this.mySQLDataBase != null) {
                try {
                    this.mySQLDataBase.close();
                } catch (Exception e) {
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
        return ModelInfo.of(table);
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
    public Model model(String table) {
        return new MySQLModel(table, this);
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

        synchronized (this) {
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
        }
        return false;
    }

    @Override
    public NativeQuery nativeQuery() {
        return new MySQLNativeQuery(mySQLDataBase);
    }

}
