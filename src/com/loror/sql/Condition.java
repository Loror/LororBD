package com.loror.sql;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Loror on 2018/2/8.
 */

public class Condition implements Comparable<Condition> {

    private String key;
    private String operator;
    private Object column;
    private int type;//0,and.1,or
    private boolean quotation;//是否允许库自动添加'
    private Set<Condition> conditions;

    public Condition(String key, String operator, Object column) {
        this(key, operator, column, 0);
    }

    public Condition(String key, String operator, Object column, int type) {
        this(key, operator, column, type, true);
    }

    public Condition(String key, String operator, Object column, int type, boolean quotation) {
        this.key = key;
        this.operator = operator;
        this.column = column;
        this.type = type;
        this.quotation = quotation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getColumn() {
        return column == null ? null : ColumnFilter.safeColumn(column);
    }

    public void setColumn(Object column) {
        this.column = column;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new TreeSet<>();
        }
        this.conditions.add(condition);
    }

    public Set<Condition> getConditions() {
        return conditions;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean withColumn) {
        StringBuilder builder = new StringBuilder();
        if (conditions != null) {
            builder.append("(");
        }
        if (ColumnFilter.isFullName(key)) {
            builder.append(key);
            builder.append(" ");
        } else {
            builder.append("`");
            builder.append(key);
            builder.append("` ");
        }
        builder.append(operator);
        if (withColumn) {
            if (column == null) {
                builder.append(" null");
            } else {
                if (quotation) {
                    builder.append(" '");
                    builder.append(ColumnFilter.safeColumn(column));
                    builder.append("'");
                } else {
                    builder.append(" ")
                            .append(column);
                }
            }
        } else {
            builder.append(" ?");
        }
        if (conditions != null) {
            for (Condition condition : conditions) {
                builder.append(condition.getType() == 0 ? " and " : " or ");
                builder.append(condition.toString(withColumn));
            }
            builder.append(")");
        }
        return builder.toString();
    }

    @Override
    public int compareTo(Condition that) {
        int compare = this.key.compareTo(that.key);
        if (compare != 0) {
            return compare;
        }
        compare = this.operator.compareTo(that.operator);
        if (compare != 0) {
            return compare;
        }
        return this.column == null ? -1 :
                that.column == null ? 1 :
                        String.valueOf(this.column).compareTo(String.valueOf(that.column));
    }
}
