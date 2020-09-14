package com.loror.sql;

import java.util.List;
import java.util.Map;

public abstract class Model<T> extends Where {

    protected Class<T> table;
    protected ModelInfo modelInfo;

    public Model(Class<T> table, ModelInfo modelInfo, ConditionBuilder conditionBuilder) {
        super(conditionBuilder);
        this.table = table;
        this.modelInfo = modelInfo;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }

    @Override
    public Model<T> where(String key, Object var) {
        super.where(key, var);
        return this;
    }

    @Override
    public Model<T> where(String key, String operation, Object var) {
        super.where(key, operation, var);
        return this;
    }

    @Override
    public Model<T> whereOr(String key, Object var) {
        super.whereOr(key, var);
        return this;
    }

    @Override
    public Model<T> whereOr(String key, String operation, Object var) {
        super.whereOr(key, operation, var);
        return this;
    }

    @Override
    public Model<T> whereIn(String key, Object[] vars) {
        super.whereIn(key, vars);
        return this;
    }

    @Override
    public Model<T> whereIn(String key, String operation, Object[] vars) {
        super.whereIn(key, operation, vars);
        return this;
    }

    @Override
    public Model<T> whereIn(String key, List<?> vars) {
        super.whereIn(key, vars);
        return this;
    }

    @Override
    public Model<T> whereIn(String key, String operation, List<?> vars) {
        super.whereIn(key, operation, vars);
        return this;
    }

    @Override
    public Model<T> whereOrIn(String key, Object[] vars) {
        super.whereOrIn(key, vars);
        return this;
    }

    @Override
    public Model<T> whereOrIn(String key, String operation, Object[] vars) {
        super.whereOrIn(key, operation, vars);
        return this;
    }

    @Override
    public Model<T> whereOrIn(String key, List<?> vars) {
        super.whereOrIn(key, vars);
        return this;
    }

    @Override
    public Model<T> whereOrIn(String key, String operation, List<?> vars) {
        super.whereOrIn(key, operation, vars);
        return this;
    }

    @Override
    public Model<T> where(OnWhere onWhere) {
        super.where(onWhere);
        return this;
    }

    @Override
    public Model<T> whereOr(OnWhere onWhere) {
        super.whereOr(onWhere);
        return this;
    }

    @Override
    public Model<T> when(boolean satisfy, OnWhere onWhere) {
        super.when(satisfy, onWhere);
        return this;
    }

    public Model<T> orderBy(String key, int order) {
        conditionBuilder.withOrder(key, order);
        return this;
    }

    public Model<T> page(int page, int size) {
        conditionBuilder.withPagination(page, size);
        return this;
    }

    /**
     * 指定字段查询
     */
    public abstract Model<T> select(String... columns);

    /**
     * 保存
     */
    public abstract void save(T entity);

    /**
     * 保存
     */
    public abstract boolean save(List<T> entities);

    /**
     * 条件删除
     */
    public abstract void delete();

    /**
     * 清空表
     */
    public abstract void clear();

    /**
     * 截断表
     */
    public abstract void truncate();

    /**
     * 修改
     */
    public abstract int update(T entity, boolean ignoreNull);

    /**
     * 修改
     */
    public abstract int update(Map<String, Object> values);

    /**
     * 条件计数
     */
    public abstract int count();

    /**
     * 条件查询
     */
    public abstract List<T> get();

    /**
     * 条件查询首条
     */
    public abstract T first();

}
