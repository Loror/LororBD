package com.loror.sql;

import java.util.Arrays;
import java.util.List;

public class Where {

    public interface OnWhere {
        void where(Where where);
    }

    protected ConditionBuilder conditionBuilder;

    public Where(ConditionBuilder conditionBuilder) {
        this.conditionBuilder = conditionBuilder;
    }

    public Where where(String key, Object var) {
        return where(key, var == null ? "is" : "=", var);
    }

    public Where where(String key, String operation, Object var) {
        conditionBuilder.addCondition(key, operation, var);
        return this;
    }

    public Where whereOr(String key, Object var) {
        return whereOr(key, var == null ? "is" : "=", var);
    }

    public Where whereOr(String key, String operation, Object var) {
        conditionBuilder.addOrCondition(key, operation, var);
        return this;
    }

    public Where whereIn(String key, Object[] vars) {
        return whereIn(key, "in", vars);
    }

    public Where whereIn(String key, String operation, Object[] vars) {
        if (vars == null || vars.length == 0) {
            return this;
        }
        return whereIn(key, operation, Arrays.asList(vars));
    }

    public Where whereIn(String key, List<?> vars) {
        return whereIn(key, "in", vars);
    }

    public Where whereIn(String key, String operation, List<?> vars) {
        if (vars == null || vars.size() == 0) {
            throw new IllegalArgumentException("in condition can not be empty");
        }
        conditionBuilder.addInCondition(key, operation, vars);
        return this;
    }

    public Where whereOrIn(String key, Object[] vars) {
        return whereOrIn(key, "in", vars);
    }

    public Where whereOrIn(String key, String operation, Object[] vars) {
        if (vars == null || vars.length == 0) {
            return this;
        }
        return whereOrIn(key, operation, Arrays.asList(vars));
    }

    public Where whereOrIn(String key, List<?> vars) {
        return whereOrIn(key, "in", vars);
    }

    public Where whereOrIn(String key, String operation, List<?> vars) {
        if (vars == null || vars.size() == 0) {
            throw new IllegalArgumentException("in condition can not be empty");
        }
        conditionBuilder.addOrInCondition(key, operation, vars);
        return this;
    }

    public Where where(OnWhere onWhere) {
        return where(onWhere, 0);
    }

    public Where whereOr(OnWhere onWhere) {
        return where(onWhere, 1);
    }

    private Where where(OnWhere onWhere, int type) {
        if (onWhere != null) {
            Where where = new Where(ConditionBuilder.create());
            onWhere.where(where);
            List<Condition> conditions = where.conditionBuilder.getConditionList();
            if (conditions.size() > 0) {
                Condition top = conditions.get(0);
                top.setType(type);
                for (Condition condition : conditions) {
                    if (condition == top) {
                        continue;
                    }
                    top.addCondition(condition);
                }
                conditionBuilder.addCondition(top, where.conditionBuilder.isHasNull());
            }
        }
        return this;
    }

    public Where when(boolean satisfy, OnWhere onWhere) {
        if (satisfy && onWhere != null) {
            onWhere.where(this);
        }
        return this;
    }

    @Override
    public String toString() {
        return conditionBuilder.toString();
    }
}
