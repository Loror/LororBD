package com.loror.sql.mysql;

import com.loror.sql.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MySQLModel extends Model {

    private MySQLClient sqlClient;
    private String model;
    private List<Join> joins;
    private String select = "*";

    public MySQLModel(String model, MySQLClient sqlClient) {
        super(ConditionRequest.build());
        this.model = model;
        this.sqlClient = sqlClient;
    }

    private String safeTable() {
        String table;
        if (ColumnFilter.isFullName(model)) {
            table = model;
        } else {
            table = "`" + model + "`";
        }
        if (joins != null) {
            for (Join join : joins) {
                table += " " + join.toString();
            }
        }
        return table;
    }

    @Override
    public Model join(String model, String on) {
        if (joins == null) {
            joins = new ArrayList<>();
        }
        joins.add(new Join(0, model, on));
        return this;
    }

    @Override
    public Model innerJoin(String model, String on) {
        if (joins == null) {
            joins = new ArrayList<>();
        }
        joins.add(new Join(3, model, on));
        return this;
    }

    @Override
    public Model leftJoin(String model, String on) {
        if (joins == null) {
            joins = new ArrayList<>();
        }
        joins.add(new Join(1, model, on));
        return this;
    }

    @Override
    public Model rightJoin(String model, String on) {
        if (joins == null) {
            joins = new ArrayList<>();
        }
        joins.add(new Join(2, model, on));
        return this;
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
     * 创建sql标识
     */
    private SQLClient.QueryIdentification buildIdentification() {
        SQLClient.QueryIdentification identification = new SQLClient.QueryIdentification();
        identification.setConditionRequest(conditionRequest);
        identification.setJoins(joins);
        identification.setSelect(select);
        identification.setModel(model);
        return identification;
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
            ModelData modelResult = sqlClient.nativeQuery(buildIdentification()).executeByReturnKeys(sql);
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
            sqlClient.nativeQuery(buildIdentification()).execute(sql);
        }
    }

    @Override
    public void save(Object entity) {
        if (entity != null) {
            if (entity instanceof ModelData) {
                ModelData modelResult = (ModelData) entity;
                if (modelResult.getModel() == null) {
                    modelResult.setModel(model);
                }
                sqlClient.nativeQuery(buildIdentification()).execute(MySQLBuilder.getInsertSql(modelResult));
            } else {
                ModelInfo.ColumnInfo idColumn = ModelInfo.of(entity.getClass()).getId();
                long id = 0;
                if (idColumn != null) {
                    Field field = idColumn.getField();
                    field.setAccessible(true);
                    try {
                        Object var = field.get(entity);
                        id = var == null ? 0 : Long.parseLong(String.valueOf(var));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                if (id == 0) {
                    insert(entity);
                } else {
                    String sql = MySQLBuilder.getUpdateSqlNoWhere(entity, ModelInfo.of(entity.getClass()), false) +
                            " where " + idColumn.getSafeName() + " = " + id;
                    sqlClient.nativeQuery(buildIdentification()).executeUpdate(sql);
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
        if (conditionRequest.getConditionCount() > 0) {
            String sql = "delete from " + safeTable() + conditionRequest.getConditions(true);
            sqlClient.nativeQuery(buildIdentification()).execute(sql);
        }
    }

    @Override
    public void delete(Object entity) {
        if (entity != null) {
            if (entity instanceof ModelData) {
                ModelData modelResult = (ModelData) entity;
                if (modelResult.getModel() == null) {
                    modelResult.setModel(model);
                }
                sqlClient.nativeQuery(buildIdentification()).execute(MySQLBuilder.getDeleteSql(modelResult));
            } else {
                ModelInfo modelInfo = ModelInfo.of(entity.getClass());
                ModelInfo.ColumnInfo idColumn = modelInfo.getId();
                long id = 0;
                if (idColumn != null) {
                    Field field = idColumn.getField();
                    field.setAccessible(true);
                    try {
                        Object var = field.get(entity);
                        id = var == null ? 0 : Long.parseLong(String.valueOf(var));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                if (id == 0) {
                    sqlClient.nativeQuery(buildIdentification()).execute(MySQLBuilder.getDeleteSql(entity, modelInfo));
                } else {
                    sqlClient.nativeQuery(buildIdentification()).execute("delete from " + safeTable() + " where " +
                            idColumn.getSafeName() + " = " + id);
                }
            }
        }
    }

    @Override
    public void clear() {
        String sql = "delete from " + safeTable();
        sqlClient.nativeQuery(buildIdentification()).execute(sql);
    }

    @Override
    public void truncate() {
        String sql = "truncate table " + safeTable();
        sqlClient.nativeQuery(buildIdentification()).execute(sql);
    }

    @Override
    public int update(Object entity, boolean ignoreNull) {
        if (entity == null) {
            return 0;
        }
        String sql = MySQLBuilder.getUpdateSqlNoWhere(entity, ModelInfo.of(entity.getClass()), ignoreNull)
                + conditionRequest.getConditionsWithoutPage(true);
        return sqlClient.nativeQuery(buildIdentification()).executeUpdate(sql);
    }

    @Override
    public int update(ModelData values) {
        if (values == null) {
            return 0;
        }
        String sql = MySQLBuilder.getUpdateSqlNoWhere(values, safeTable())
                + conditionRequest.getConditionsWithoutPage(true);
        return sqlClient.nativeQuery(buildIdentification()).executeUpdate(sql);
    }

    @Override
    public int count() {
        String sql;
        if (conditionRequest.getConditionCount() == 0) {
            sql = "select count(1) from " + safeTable();
        } else {
            sql = "select count(1) from " + safeTable() + conditionRequest.getConditionsWithoutPage(true);
        }
        ModelDataList results = sqlClient.nativeQuery(buildIdentification()).executeQuery(sql);
        return results.size() == 0 ? 0 : results.get(0).getInt("count(1)", 0);
    }

    @Override
    public ModelDataList get() {
        String sql = "select " + this.select + " from " + safeTable() + conditionRequest.getConditionsWithoutPage(true)
                + conditionRequest.getGroupBy() + (conditionRequest.getPage() == null ? "" : " " + conditionRequest.getPage().toString());
        return sqlClient.nativeQuery(buildIdentification()).executeQuery(sql);
    }

    @Override
    public ModelData first() {
        String sql = "select " + this.select + " from " + safeTable()
                + conditionRequest.getConditionsWithoutPage(true) + conditionRequest.getGroupBy() + " limit 0,1";
        ModelDataList results = sqlClient.nativeQuery(buildIdentification()).executeQuery(sql);
        return results.size() == 0 ? new ModelData(true) : results.get(0);
    }
}
