package com.loror.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Loror on 2018/2/8.
 */

public class ConditionRequest {

    private Set<Condition> conditions = new TreeSet<>();
    private Set<Order> orders = new TreeSet<>();
    private Page page;
    private boolean hasNull;
    private String groupName;
    private Set<Having> havings = new TreeSet<>();

    private ConditionRequest() {

    }

    public static ConditionRequest build() {
        return new ConditionRequest();
    }

    /**
     * 获取条件数量
     */
    public int getConditionCount() {
        return conditions.size();
    }

    public Set<Condition> getConditionList() {
        return conditions;
    }

    public Set<Order> getOrderList() {
        return orders;
    }

    public Page getPage() {
        return page;
    }

    public boolean isHasNull() {
        return hasNull;
    }

    /**
     * 分页
     */
    public ConditionRequest withPagination(int page, int number) {
        this.page = new Page(page, number);
        return this;
    }

    /**
     * 排序条件条件
     */
    public ConditionRequest withOrder(String key, int orderType) {
        orders.add(new Order(key, orderType));
        return this;
    }

    /**
     * 增加条件
     */
    public ConditionRequest addCondition(String key, String operator, Object column) {
        return addCondition(key, operator, column, true);
    }

    /**
     * list条件拼接
     */
    private String getListCondition(List<?> columns) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0; i < columns.size(); i++) {
            Object column = columns.get(i);
            if (column != null) {
                builder.append("'")
                        .append(ColumnFilter.safeColumn(columns.get(i)))
                        .append("'");
                if (i != columns.size() - 1) {
                    builder.append(",");
                }
            }
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     * 增加in条件
     */
    public ConditionRequest addInCondition(String key, String operator, List<?> columns) {
        if (columns.size() == 1) {
            return addCondition(key, "not in".equalsIgnoreCase(operator) ? "<>" : "=", columns.get(0), true);
        } else {
            return addCondition(key, operator, getListCondition(columns), false);
        }
    }

    /**
     * 增加条件
     */
    public ConditionRequest addCondition(String key, String operator, Object column, boolean quotation) {
        if (column == null) {
            hasNull = true;
        }
        conditions.add(new Condition(key, operator, column, 0, quotation));
        return this;
    }

    /**
     * 增加条件
     */
    public ConditionRequest addOrCondition(String key, String operator, Object column) {
        return addOrCondition(key, operator, column, true);
    }

    /**
     * 增加in条件
     */
    public ConditionRequest addOrInCondition(String key, String operator, List<?> columns) {
        if (columns.size() == 1) {
            return addOrCondition(key, "not in".equalsIgnoreCase(operator) ? "<>" : "=", columns.get(0), true);
        } else {
            return addOrCondition(key, operator, getListCondition(columns), false);
        }
    }

    /**
     * 增加条件
     */
    public ConditionRequest addOrCondition(String key, String operator, Object column, boolean quotation) {
        if (column == null) {
            hasNull = true;
        }
        conditions.add(new Condition(key, operator, column, 1, quotation));
        return this;
    }

    /**
     * 增加条件
     */
    public ConditionRequest addCondition(Condition condition, boolean hasNull) {
        if (hasNull) {
            this.hasNull = true;
        }
        conditions.add(condition);
        return this;
    }

    /**
     * 设置group
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * 增加条件
     */
    public ConditionRequest addHaving(Having having) {
        havings.add(having);
        return this;
    }

    /**
     * 获取排序语句
     */
    public String getOrders() {
        StringBuilder builder = new StringBuilder();
        boolean flag = false;
        for (Order order : orders) {
            if (!flag) {
                builder.append(order.toString());
                flag = true;
            } else {
                builder.append(order.toString().replace("order by ", ","));
            }
        }
        return builder.toString();
    }

    /**
     * 获取group
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * 获取having
     */
    public Set<Having> getHavings() {
        return havings;
    }

    /**
     * 获取条件语句
     */
    public String getConditions(boolean withColumn) {
        return getConditionsWithoutPage(withColumn) + (page == null ? "" : " " + page.toString());
    }

    /**
     * 获取条件语句
     */
    public String getConditionsWithoutPage(boolean withColumn) {
        StringBuilder builder = new StringBuilder();
        boolean flag = false;
        for (Condition condition : conditions) {
            if (flag) {
                builder.append(condition.getType() == 0 ? " and " : " or ");
            } else {
                builder.append(" where ");
                flag = true;
            }
            builder.append(condition.toString(withColumn));
        }
        String order = getOrders();
        if (order.length() > 0) {
            builder.append(" ");
            builder.append(order);
        }
        return builder.toString();
    }

    /**
     * 获取条件值数组
     */
    public String[] getColumnArray() {
        List<String> array = new ArrayList<>();
        if (conditions.size() > 0) {
            add(array, conditions);
        }
        return array.toArray(new String[0]);
    }

    private void add(List<String> array, Set<Condition> conditions) {
        for (Condition condition : conditions) {
            array.add(condition.getColumn());
            if (condition.getConditions() != null) {
                add(array, condition.getConditions());
            }
        }
    }

    /**
     * 获取having
     */
    public String getGroupBy() {
        if (groupName == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Having having : havings) {
            builder.append(having)
                    .append(" and ");
        }
        if (builder.length() > 0) {
            builder.delete(builder.length() - 5, builder.length());
        }
        String group = ColumnFilter.isFullName(groupName) ?
                groupName
                : ("`" + groupName + "`");
        String having = builder.toString();
        return " group by " + group + (having.length() > 0 ? (" having " + having) : "");
    }

    @Override
    public String toString() {
        return getConditions(true);
    }
}
