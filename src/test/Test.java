package test;

import com.loror.sql.SQLClient;
import com.loror.sql.mysql.MySQLClient;

public class Test {

    public static void main(String[] args) {

        SQLClient sqlClient = new MySQLClient("jdbc:mysql://localhost:3306/test", "root", "11231123");
        TestTable test = new TestTable();
        test.name = "name";
        test.email = "name@qq.com";
        test.random = (int) (Math.random() * 101);

//        sqlClient.model(TestTable.class).truncate();
        sqlClient.model(TestTable.class).save(test);

        System.out.println(test);

        System.out.println(sqlClient.model(TestTable.class)
                .select("name", "random", "id")
                .where("id", "2")
                .first());

//        sqlClient.model(TestTable.class)
//                .where("name", "name")
//                .delete();
//        sqlClient.close();

        System.out.println(sqlClient.model(TestTable.class).count());
        sqlClient.close();
    }
}
