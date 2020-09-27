package com.loror.sql;

public class SQLException extends RuntimeException {

    private Exception e;

    public SQLException(Exception e) {
        super(e.getMessage());
        this.e = e;
    }

    @Override
    public void printStackTrace() {
        e.printStackTrace();
    }

    public Exception e() {
        return e;
    }
}
