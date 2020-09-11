package com.loror;

import com.loror.sql.Column;
import com.loror.sql.Id;
import com.loror.sql.Table;

@Table(name = "test", checkTable = true)
public class TestTable {

    @Id(returnKey = true)
    public int id;
    @Column
    public String name;
    @Column
    public String email;
    @Column(defaultValue = "3")
    public int random;

    @Override
    public String toString() {
        return "TestTable{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", random=" + random +
                '}';
    }
}
