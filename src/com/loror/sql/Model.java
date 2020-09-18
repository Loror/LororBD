package com.loror.sql;

import java.util.List;
import java.util.Map;

public abstract class Model extends Where {

    public Model(ConditionRequest conditionRequest) {
        super(conditionRequest);
    }

    @Override
    public Model where(String key, Object var) {
        super.where(key, var);
        return this;
    }

    @Override
    public Model where(String key, String operation, Object var) {
        super.where(key, operation, var);
        return this;
    }

    @Override
    public Model whereOr(String key, Object var) {
        super.whereOr(key, var);
        return this;
    }

    @Override
    public Model whereOr(String key, String operation, Object var) {
        super.whereOr(key, operation, var);
        return this;
    }

    @Override
    public Model whereIn(String key, Object[] vars) {
        super.whereIn(key, vars);
        return this;
    }

    @Override
    public Model whereIn(String key, String operation, Object[] vars) {
        super.whereIn(key, operation, vars);
        return this;
    }

    @Override
    public Model whereIn(String key, List<?> vars) {
        super.whereIn(key, vars);
        return this;
    }

    @Override
    public Model whereIn(String key, String operation, List<?> vars) {
        super.whereIn(key, operation, vars);
        return this;
    }

    @Override
    public Model whereOrIn(String key, Object[] vars) {
        super.whereOrIn(key, vars);
        return this;
    }

    @Override
    public Model whereOrIn(String key, String operation, Object[] vars) {
        super.whereOrIn(key, operation, vars);
        return this;
    }

    @Override
    public Model whereOrIn(String key, List<?> vars) {
        super.whereOrIn(key, vars);
        return this;
    }

    @Override
    public Model whereOrIn(String key, String operation, List<?> vars) {
        super.whereOrIn(key, operation, vars);
        return this;
    }

    @Override
    public Model where(OnWhere onWhere) {
        super.where(onWhere);
        return this;
    }

    @Override
    public Model whereOr(OnWhere onWhere) {
        super.whereOr(onWhere);
        return this;
    }

    @Override
    public Model when(boolean satisfy, OnWhere onWhere) {
        super.when(satisfy, onWhere);
        return this;
    }

    public Model groupBy(String key) {
        if (key != null && key.length() > 0) {
            conditionRequest.setGroupName(key);
        }
        return this;
    }

    public Model having(String key, Object var) {
        return having(key, var == null ? "is" : "=", var);
    }

    public Model having(String key, String operation, Object var) {
        conditionRequest.addHaving(new Having(key, operation, var));
        return this;
    }

    public Model orderBy(String key, int order) {
        conditionRequest.withOrder(key, order);
        return this;
    }

    public Model page(int page, int size) {
        if (page <= 0) {
            throw new IllegalArgumentException("page cannot be zero or minus");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size cannot be zero or minus");
        }
        conditionRequest.withPagination(page, size);
        return this;
    }

    /**
     * 连表
     */
    public abstract Model join(String model, String on);

    /**
     * 左连表
     */
    public abstract Model leftJoin(String model, String on);

    /**
     * 右连表
     */
    public abstract Model rightJoin(String model, String on);

    /**
     * 指定字段查询
     */
    public abstract Model select(String... columns);

    /**
     * 指定字段查询
     */
    public abstract Model select(String column);

    /**
     * 保存
     */
    public abstract void save(Object entity);

    /**
     * 保存
     */
    public abstract boolean save(List<?> entities);

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
    public abstract int update(Object entity, boolean ignoreNull);

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
    public abstract ModelResultList get();

    /**
     * 条件查询首条
     */
    public abstract ModelResult first();

}
