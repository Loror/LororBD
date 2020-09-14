package test;

import com.loror.sql.Column;
import com.loror.sql.Id;
import com.loror.sql.Table;

@Table(name = "test")
public class TestTable {

    @Id(length = 11, returnKey = true)
    public int id;
    @Column
    public String name;
    @Column
    public String email;
    @Column(name = "random", defaultValue = "3")
    public int count;
    @Column(length = 1, defaultValue = "0")
    public int group;

    @Override
    public String toString() {
        return "TestTable{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", count=" + count +
                ", group=" + group +
                '}';
    }
}
