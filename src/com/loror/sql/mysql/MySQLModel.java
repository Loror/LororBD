package com.loror.sql.mysql;

import com.loror.sql.*;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class MySQLModel extends Model {

    private MySQLClient sqlClient;
    private String table;
    private String select = "*";

    public MySQLModel(String table, MySQLClient sqlClient) {
        super(ConditionBuilder.create());
        this.table = table;
        this.sqlClient = sqlClient;
    }

    @Override
    public Model select(String... columns) {
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
    public Model select(String columns) {
        if (columns != null && columns.length() > 0) {
            if (ColumnFilter.isFullName(columns)) {
                this.select = columns;
            } else {
                this.select = "`" + columns + "`";
            }
        }
        return this;
    }

    /**
     * 插入
     */
    protected void insert(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            ModelInfo modelInfo = ModelInfo.of(entity.getClass());
            ModelInfo.ColumnInfo id = modelInfo.getId();
            boolean returnId = id != null && id.isReturnKey();
            sqlClient.getDatabase().getPst(MySQLBuilder.getInsertSql(entity, modelInfo), returnId, pst -> {
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
            sqlClient.getDatabase().getPst(MySQLBuilder.getUpdateSql(entity, ModelInfo.of(entity.getClass())), false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(Object entity) {
        if (entity != null) {
            ModelInfo.ColumnInfo idColumn = ModelInfo.of(entity.getClass()).getId();
            if (idColumn == null) {
                insert(entity);
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
                    insert(entity);
                } else {
                    updateById(entity);
                }
            }
        }
    }

    @Override
    public boolean save(List<?> entities) {
        return sqlClient.transaction(() -> {
            for (Object t : entities) {
                save(t);
            }
        });
    }

    @Override
    public void delete() {
        if (conditionBuilder.getConditionCount() > 0) {
            try {
                sqlClient.getDatabase().getPst("delete from `" + table + "`" + conditionBuilder.getConditions(true), false, pst -> {
                    pst.execute();
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void clear() {
        try {
            sqlClient.getDatabase().getPst("delete from `" + table + "`", false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void truncate() {
        try {
            sqlClient.getDatabase().getPst("truncate table `" + table + "`", false, pst -> {
                pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int update(Object entity, boolean ignoreNull) {
        if (entity == null) {
            return 0;
        }
        int[] updates = new int[1];
        try {
            sqlClient.getDatabase().getPst(MySQLBuilder.getUpdateSqlNoWhere(entity, ModelInfo.of(entity.getClass()), ignoreNull)
                    + conditionBuilder.getConditionsWithoutPage(true), false, pst -> {
                updates[0] = pst.executeUpdate();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updates[0];
    }

    @Override
    public int update(Map<String, Object> values) {
        if (values == null) {
            return 0;
        }
        int[] updates = new int[1];
        try {
            sqlClient.getDatabase().getPst(MySQLBuilder.getUpdateSqlNoWhere(values, table)
                    + conditionBuilder.getConditionsWithoutPage(true), false, pst -> {
                updates[0] = pst.executeUpdate();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updates[0];
    }

    @Override
    public int count() {
        int[] count = {0};
        try {
            String sql = null;
            if (conditionBuilder.getConditionCount() == 0) {
                sql = "select count(1) from `" + table + "`";
            } else {
                sql = "select count(1) from `" + table + "`" + conditionBuilder.getConditionsWithoutPage(true);
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

    private String getGroup() {
        return groupName == null ? "" : (" group by " + groupName + (having != null ? (" having (" + having + ")") : ""));
    }

    @Override
    public ModelResultList get() {
        ModelResultList entitys = new ModelResultList();
        try {
            sqlClient.getDatabase().getPst("select " + this.select + " from `" + table + "`" + conditionBuilder.getConditionsWithoutPage(true)
                    + getGroup() + (conditionBuilder.getPage() == null ? "" : " " + conditionBuilder.getPage().toString()), false, pst -> {
                ResultSet cursor = pst.executeQuery();
                List<ModelResult> modelResults = MySQLResult.find(cursor);
                cursor.close();
                entitys.addAll(modelResults);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entitys;
    }

    @Override
    public ModelResult first() {
        ModelResult[] entity = new ModelResult[]{null};
        try {
            sqlClient.getDatabase().getPst("select " + this.select + " from `" + table + "`"
                    + conditionBuilder.getConditionsWithoutPage(true) + getGroup() + " limit 0,1", false, pst -> {
                ResultSet cursor = pst.executeQuery();
                List<ModelResult> modelResults = MySQLResult.find(cursor);
                cursor.close();
                if (modelResults.size() > 0) {
                    entity[0] = modelResults.get(0);
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity[0];
    }

    public int lastInsertId(Class<?> table) {
        int[] id = {-1};
        try {
            sqlClient.getDatabase().getPst(MySQLBuilder.getLastIdSql(ModelInfo.of(table)), false, pst -> {
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
}
