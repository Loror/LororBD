package com.loror.sql;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class Model<T> implements Where {

    protected Class<T> table;
    protected ModelInfo modelInfo;
    protected ConditionBuilder conditionBuilder;

    public Model(Class<T> table, ModelInfo modelInfo, ConditionBuilder conditionBuilder) {
        this.table = table;
        this.modelInfo = modelInfo;
        this.conditionBuilder = conditionBuilder;
    }

    public ModelInfo getModelInfo() {
        return modelInfo;
    }

    @Override
    public Model<T> where(String key, Object var) {
        return where(key, var == null ? "is" : "=", var);
    }

    @Override
    public Model<T> where(String key, String operation, Object var) {
        conditionBuilder.addCondition(key, operation, var);
        return this;
    }

    @Override
    public Model<T> whereOr(String key, Object var) {
        return whereOr(key, var == null ? "is" : "=", var);
    }

    @Override
    public Model<T> whereOr(String key, String operation, Object var) {
        conditionBuilder.addOrCondition(key, operation, var);
        return this;
    }

    @Override
    public Model<T> whereIn(String key, Object[] vars) {
        return whereIn(key, "in", vars);
    }

    @Override
    public Model<T> whereIn(String key, String operation, Object[] vars) {
        if (vars == null || vars.length == 0) {
            return this;
        }
        return whereIn(key, operation, Arrays.asList(vars));
    }

    @Override
    public Model<T> whereIn(String key, List<?> vars) {
        return whereIn(key, "in", vars);
    }

    @Override
    public Model<T> whereIn(String key, String operation, List<?> vars) {
        if (vars == null || vars.size() == 0) {
            throw new IllegalArgumentException("in condition can not be empty");
        }
        conditionBuilder.addInCondition(key, operation, vars);
        return this;
    }

    @Override
    public Model<T> whereOrIn(String key, Object[] vars) {
        return whereOrIn(key, "in", vars);
    }

    @Override
    public Model<T> whereOrIn(String key, String operation, Object[] vars) {
        if (vars == null || vars.length == 0) {
            return this;
        }
        return whereOrIn(key, operation, Arrays.asList(vars));
    }

    @Override
    public Model<T> whereOrIn(String key, List<?> vars) {
        return whereOrIn(key, "in", vars);
    }

    @Override
    public Model<T> whereOrIn(String key, String operation, List<?> vars) {
        if (vars == null || vars.size() == 0) {
            throw new IllegalArgumentException("in condition can not be empty");
        }
        conditionBuilder.addOrInCondition(key, operation, vars);
        return this;
    }

    @Override
    public Model<T> where(OnWhere onWhere) {
        return where(onWhere, 0);
    }

    @Override
    public Model<T> whereOr(OnWhere onWhere) {
        return where(onWhere, 1);
    }

    private Model<T> where(OnWhere onWhere, int type) {
        if (onWhere != null) {
            Model<T> model = newModel();
            onWhere.where(model);
            List<Condition> conditions = model.conditionBuilder.getConditionList();
            if (conditions.size() > 0) {
                Condition top = conditions.get(0);
                top.setType(type);
                for (Condition condition : conditions) {
                    if (condition == top) {
                        continue;
                    }
                    top.addCondition(condition);
                }
                conditionBuilder.addCondition(top, model.conditionBuilder.isHasNull());
            }
        }
        return this;
    }

    @Override
    public Model<T> when(boolean satisfy, OnWhere onWhere) {
        if (satisfy && onWhere != null) {
            onWhere.where(this);
        }
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

    protected abstract Model<T> newModel();

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
    public abstract void update(T entity, boolean ignoreNull);

    /**
     * 修改
     */
    public abstract void update(HashMap<String, Object> values);

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

    @Override
    public String toString() {
        return conditionBuilder.toString();
    }
}
