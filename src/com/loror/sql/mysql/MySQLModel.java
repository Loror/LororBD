package com.loror.sql.mysql;

import com.loror.sql.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class MySQLModel extends Model {

    private MySQLClient sqlClient;
    private String table;
    private String select = "*";

    public MySQLModel(String table, MySQLClient sqlClient) {
        super(ConditionBuilder.create());
        if (ColumnFilter.isFullName(table)) {
            this.table = table;
        } else {
            this.table = "`" + table + "`";
        }
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
        ModelInfo modelInfo = ModelInfo.of(entity.getClass());
        ModelInfo.ColumnInfo id = modelInfo.getId();
        boolean returnId = id != null && id.isReturnKey();
        String sql = MySQLBuilder.getInsertSql(entity, modelInfo);
        if (returnId) {
            ModelResult modelResult = sqlClient.nativeQuery().executeByReturnKeys(sql);
            List<String> keys = modelResult.keys();
            if (keys.size() > 0) {
                long num = modelResult.getLong(keys.get(0), 0);
                if (num != 0) {
                    Class<?> type = id.getTypeClass();
                    Field field = id.getField();
                    if (type == int.class || type == Integer.class) {
                        field.setAccessible(true);
                        try {
                            field.set(entity, (int) num);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (type == long.class || type == Long.class) {
                        field.setAccessible(true);
                        try {
                            field.set(entity, num);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            sqlClient.nativeQuery().execute(sql);
        }
    }

    /**
     * 根据id更新数据
     */
    protected void updateById(Object entity) {
        if (entity == null) {
            return;
        }
        String sql = MySQLBuilder.getUpdateSql(entity, ModelInfo.of(entity.getClass()));
        sqlClient.nativeQuery().executeUpdate(sql);
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
            String sql = "delete from " + table + conditionBuilder.getConditions(true);
            sqlClient.nativeQuery().execute(sql);
        }
    }

    @Override
    public void clear() {
        String sql = "delete from " + table;
        sqlClient.nativeQuery().execute(sql);
    }

    @Override
    public void truncate() {
        String sql = "truncate table " + table;
        sqlClient.nativeQuery().execute(sql);
    }

    @Override
    public int update(Object entity, boolean ignoreNull) {
        if (entity == null) {
            return 0;
        }
        String sql = MySQLBuilder.getUpdateSqlNoWhere(entity, ModelInfo.of(entity.getClass()), ignoreNull)
                + conditionBuilder.getConditionsWithoutPage(true);
        return sqlClient.nativeQuery().executeUpdate(sql);
    }

    @Override
    public int update(Map<String, Object> values) {
        if (values == null) {
            return 0;
        }
        String sql = MySQLBuilder.getUpdateSqlNoWhere(values, table)
                + conditionBuilder.getConditionsWithoutPage(true);
        return sqlClient.nativeQuery().executeUpdate(sql);
    }

    @Override
    public int count() {
        String sql;
        if (conditionBuilder.getConditionCount() == 0) {
            sql = "select count(1) from " + table;
        } else {
            sql = "select count(1) from " + table + conditionBuilder.getConditionsWithoutPage(true);
        }
        ModelResultList results = sqlClient.nativeQuery().executeQuery(sql);
        return results.size() == 0 ? 0 : results.get(0).getInt("count(1)", 0);
    }

    private String getGroup() {
        return groupName == null ? "" : (" group by " + groupName + (having != null ? (" having (" + having + ")") : ""));
    }

    @Override
    public ModelResultList get() {
        String sql = "select " + this.select + " from " + table + conditionBuilder.getConditionsWithoutPage(true)
                + getGroup() + (conditionBuilder.getPage() == null ? "" : " " + conditionBuilder.getPage().toString());
        return sqlClient.nativeQuery().executeQuery(sql);
    }

    @Override
    public ModelResult first() {
        String sql = "select " + this.select + " from " + table
                + conditionBuilder.getConditionsWithoutPage(true) + getGroup() + " limit 0,1";
        ModelResultList results = sqlClient.nativeQuery().executeQuery(sql);
        return results.size() == 0 ? new ModelResult(true) : results.get(0);
    }
}
