package com.loror.sql.mysql;

import com.loror.sql.*;

import java.sql.*;
import java.sql.SQLException;
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
    private ExceptionHandler exceptionHandler;
    private SQLCache sqlCache;
    private QueryIdentification cacheIdentification;

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
        mySQLDataBase = new MySQLDataBase(url, name, password) {
            private QueryIdentification identification;

            @Override
            public void onException(com.loror.sql.SQLException e) {
                MySQLClient.this.onException(e);
            }

            @Override
            public void onSql(boolean connect, String sql) {
                if (logListener != null) {
                    logListener.log(connect, sql);
                }
            }

            @Override
            public ModelDataList beforeQuery(String sql) {
                identification = buildQueryIdentification(sql);
                return sqlCache == null ? null : sqlCache.beforeQuery(identification);
            }

            @Override
            public void beforeExecute(String sql) {
                identification = buildQueryIdentification(sql);
            }

            @Override
            public boolean execute(String sql) {
                boolean result = super.execute(sql);
                if (sqlCache != null) {
                    sqlCache.onExecute(identification);
                }
                return result;
            }

            @Override
            public int executeUpdate(String sql) {
                int result = super.executeUpdate(sql);
                if (sqlCache != null) {
                    sqlCache.onExecute(identification);
                }
                return result;
            }

            @Override
            public ModelData executeByReturnKeys(String sql) {
                ModelData result = super.executeByReturnKeys(sql);
                result.setModel(identification.getModel());
                if (sqlCache != null) {
                    sqlCache.onExecute(identification);
                }
                return result;
            }

            @Override
            public ModelDataList executeQuery(String sql) {
                ModelDataList results = super.executeQuery(sql);
                for (ModelData result : results) {
                    result.setModel(identification.getModel());
                }
                if (sqlCache != null) {
                    sqlCache.onExecuteQuery(identification, results);
                }
                return results;
            }
        };
    }

    /**
     * 构建QueryIdentification
     */
    private QueryIdentification buildQueryIdentification(String sql) {
        QueryIdentification identification;
        if (cacheIdentification == null) {
            identification = new QueryIdentification();
            identification.setNative(true);
        } else {
            identification = cacheIdentification;
            cacheIdentification = null;
        }
        identification.setSql(sql);
        return identification;
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
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void setSQLCache(SQLCache sqlCache) {
        this.sqlCache = sqlCache;
    }

    @Override
    public void reStart() {
        if (this.mySQLDataBase == null || this.mySQLDataBase.isClosed()) {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.mySQLDataBase = null;
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.mySQLDataBase == null || this.mySQLDataBase.isClosed();
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
        mySQLDataBase.execute(MySQLBuilder.getCreateSql(getModel(table)));
    }

    @Override
    public void dropTable(Class<?> table) {
        mySQLDataBase.execute(MySQLBuilder.getDropTableSql(getModel(table)));
    }

    @Override
    public void changeTableIfColumnAdd(Class<?> table) {
        if (execTableUpdated.contains(table)) {
            return;
        }
        execTableUpdated.add(table);
        ModelInfo modelInfo = getModel(table);
        List<String> columnNames = new ArrayList<>();
        mySQLDataBase.getPst("select * from " + modelInfo.getSafeTableName() + " limit 1", false, preparedStatement -> {
            ResultSet cursor = preparedStatement.executeQuery();
            ResultSetMetaData data = cursor.getMetaData();
            for (int i = 0; i < data.getColumnCount(); i++) {
                columnNames.add(data.getColumnName(i + 1));
            }
            cursor.close();
        });
        if (columnNames.size() == 0) {
            return;
        }
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

        List<String> different = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
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
        transaction(() -> {
            for (int i = 0; i < newColumnNames.size(); i++) {
                String newColumnName = newColumnNames.get(i);
                ModelInfo.ColumnInfo columnInfo = columnHashMap.get(newColumnName);
                String sql = "alter table " + modelInfo.getSafeTableName() + " add column " + columnInfo.getSafeName() + " " + columnInfo.getType();
                if (columnInfo.isNotNull()) {
                    sql += " NOT NULL";
                }
                String defaultValue = columnInfo.getDefaultValue();
                if (defaultValue != null && defaultValue.length() > 0) {
                    sql += " DEFAULT " + ColumnFilter.safeValue(defaultValue);
                }
                mySQLDataBase.execute(sql);
            }
        });

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
            } catch (SQLException e) {
                onException(new com.loror.sql.SQLException(e));
            }
            try {
                runnable.run();
                try {
                    connection.commit();
                } catch (SQLException e) {
                    onException(new com.loror.sql.SQLException(e));
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    onException(new com.loror.sql.SQLException(e));
                }
            }
        }
        return false;
    }

    protected void onException(com.loror.sql.SQLException e) {
        if (exceptionHandler != null) {
            exceptionHandler.handle(e);
        } else {
            throw e;
        }
    }

    @Override
    public SQLDataBase nativeQuery() {
        return mySQLDataBase;
    }

    /**
     * 获取查询，传递查询标识
     */
    protected SQLDataBase nativeQuery(QueryIdentification cacheIdentification) {
        this.cacheIdentification = cacheIdentification;
        return mySQLDataBase;
    }

}
