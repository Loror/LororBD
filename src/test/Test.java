package test;

import com.loror.sql.SQLClient;
import com.loror.sql.Where;
import com.loror.sql.mysql.MySQLClient;

import java.util.HashMap;

public class Test {

    public static void main(String[] args) {

        SQLClient sqlClient = new MySQLClient("jdbc:mysql://localhost:3306/test", "root", "11231123");
        TestTable test = new TestTable();
//        test.name = "name";
//        test.email = "name@qq.com";
//        test.random = (int) (Math.random() * 101);

//        sqlClient.model(TestTable.class).truncate();
//        HashMap<String, Object> values = new HashMap<>();
//        values.put("name", "loror");
//        sqlClient.model(TestTable.class)
//                .where("id", 3)
//                .update(values);
//
//        System.out.println(test);

        System.out.println(sqlClient.model(TestTable.class)
                .select("name", "random", "id")
                .where(where -> {
                    where.where("id", 1)
                            .whereOr("id", 3);
                })
                .first());

//        sqlClient.model(TestTable.class)
//                .where("name", "name")
//                .delete();
//        sqlClient.close();

        System.out.println(sqlClient.model(TestTable.class).count());
        sqlClient.close();
    }
}
