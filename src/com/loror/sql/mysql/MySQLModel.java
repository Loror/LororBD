package com.loror.sql.mysql;

import com.loror.sql.ConditionBuilder;
import com.loror.sql.Model;
import com.loror.sql.ModelInfo;
import com.loror.sql.TableFinder;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySQLModel<T> extends Model<T> {

    private MySQLClient sqlClient;
    private String select = "*";

    public MySQLModel(Class<T> table, MySQLClient sqlClient, ModelInfo modelInfo) {
        super(table, modelInfo, ConditionBuilder.create());
        this.sqlClient = sqlClient;
    }

    @Override
    public Model<T> select(String... columns) {
        if (columns != null && columns.length > 0) {
            String builder = "";
            for (String column : columns) {
                builder += "`" + column + "`,";
            }
            this.select = builder.substring(0, builder.length() - 1);
        }
        return this;
    }

    @Override
    public void save(T entity) {
        if (entity != null) {
            ModelInfo.ColumnInfo idColumn = modelInfo.getId();
            if (idColumn == null) {
                sqlClient.insert(entity);
            } else {
                long id = 0;
                Field field = idColumn.getField();
                field.setAccessible(true);
                try {
                    Object var = field.get(entity);
                    id = var == null ? 0 : Long.parseLong(String.valueOf(var));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (id == 0) {
                    sqlClient.insert(entity);
                } else {
                    sqlClient.updateById(entity);
                }
            }
        }
    }

    @Override
    public boolean save(List<T> entities) {
        return sqlClient.transaction(() -> {
            for (T t : entities) {
                save(t);
            }
        });
    }

    @Override
    public void delete() {
        if (conditionBuilder.getConditionCount() > 0) {
            try {
                sqlClient.getDatabase().getPst("delete from " + modelInfo.getSafeTableName() + conditionBuilder.getConditions(true), false, pst -> {
                    pst.execute();
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void clear() {
        sqlClient.deleteAll(table);
    }

    @Override
    public void truncate() {
        sqlClient.deleteAll(table);
        try {
            sqlClient.getDatabase().getPst("truncate table " + modelInfo.getSafeTableName() + conditionBuilder.getConditions(true), false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(T entity, boolean ignoreNull) {
        if (entity == null) {
            return;
        }
        try {
            sqlClient.getDatabase().getPst(TableFinder.getUpdateSqlNoWhere(entity, modelInfo, ignoreNull)
                    + conditionBuilder.getConditionsWithoutPage(true), false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int count() {
        int[] count = {0};
        try {
            String sql = null;
            if (conditionBuilder.getConditionCount() == 0) {
                sql = "select count(1) from " + modelInfo.getSafeTableName();
            } else {
                sql = "select count(1) from " + modelInfo.getSafeTableName() + conditionBuilder.getConditionsWithoutPage(true);
            }
            sqlClient.getDatabase().getPst(sql, false, pst -> {
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

    @Override
    public List<T> get() {
        List<T> entitys = new ArrayList<>();
        try {
            sqlClient.getDatabase().getPst("select " + this.select + " from " + modelInfo.getSafeTableName() + conditionBuilder.getConditions(true), false, pst -> {
                ResultSet cursor = pst.executeQuery();
                while (cursor.next()) {
                    T entity = null;
                    try {
                        entity = (T) modelInfo.getTableObject();
                        TableFinder.find(entity, cursor);
                        entitys.add(entity);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (entity == null) {
                            throw new IllegalArgumentException(table.getSimpleName() + " have no non parametric constructor");
                        }
                    }
                }
                cursor.close();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entitys;
    }

    @Override
    public T first() {
        Object[] entity = new Object[]{null};
        try {
            sqlClient.getDatabase().getPst("select " + this.select + " from " + modelInfo.getSafeTableName()
                    + conditionBuilder.getConditionsWithoutPage(true) + " limit 0,2", false, pst -> {
                ResultSet cursor = pst.executeQuery();
                if (cursor.next()) {
                    try {
                        entity[0] = modelInfo.getTableObject();
                        TableFinder.find(entity[0], cursor);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (entity[0] == null) {
                            throw new IllegalArgumentException(table.getSimpleName() + " have no non parametric constructor");
                        }
                    }
                }
                cursor.close();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (T) entity[0];
    }

    public int lastInsertId(Class<?> table) {
        int[] id = {-1};
        try {
            sqlClient.getDatabase().getPst(TableFinder.getLastIdSql(modelInfo), false, pst -> {
                ResultSet cursor = pst.executeQuery();
                if (cursor.next()) {
                    id[0] = cursor.getInt(1);
                }
                cursor.close();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id[0];
    }

    @Override
    protected Model<T> newModel() {
        return new MySQLModel<T>(table, sqlClient, modelInfo);
    }
}
