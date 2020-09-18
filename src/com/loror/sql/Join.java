package com.loror.sql;

public class Join {

    private int type;//0,join,1,left join,2,right join
    private String model;
    private String on;

    public Join(int type, String model, String on) {
        this.type = type;
        this.model = model;
        this.on = on;
    }

    public int getType() {
        return type;
    }

    public String getModel() {
        return model;
    }

    public String getOn() {
        return on;
    }

    @Override
    public String toString() {
        return (type == 1 ? "left " : type == 2 ? "right " : "") + "join " +
                (ColumnFilter.isFullName(model) ? model : ("`" + model + "`")) +
                " on " + on;
    }
}
