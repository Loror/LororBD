package test;

import com.loror.sql.ModelResult;
import com.loror.sql.ModelResultList;
import com.loror.sql.SQLClient;
import com.loror.sql.Where;
import com.loror.sql.mysql.MySQLClient;

import java.util.HashMap;
import java.util.List;

public class Test {

    public static void main(String[] args) {

        try (SQLClient sqlClient = new MySQLClient("jdbc:mysql://localhost:3306/test", "root", "11231123")) {
            TestTable test = new TestTable();
//        test.name = "name";
//        test.email = "name@qq.com";
//        test.random = (int) (Math.random() * 101);

//        sqlClient.model(TestTable.class).truncate();
//        HashMap<String, Object> values = new HashMap<>();
//        values.put("name", "loror1");
//        sqlClient.model("test")
//                .where("id", 2)
//                .update(values);
//
//        System.out.println(test);

            test = sqlClient.model("test")
                    .select("group")
                    .where(where -> {
                        where.where("id", 1)
                                .whereOr("id", 3);
                    })
                    .first()
                    .object(TestTable.class);

            System.out.println(test);

            System.out.println("id==>" + sqlClient.model("test")
                    .select("min(id) as min,max(id) as max")
                    .where("id", ">", 0)
                    .first());

            ModelResultList modelResults = sqlClient.model("test")
                    .select("sum(id)")
                    .where("id", "<>", 0)
                    .groupBy("`group`")
                    .having("sum(id)", ">", 1)
                    .page(1, 2)
                    .get();

            System.out.println("=============================");
            for (ModelResult modelResult : modelResults) {
                System.out.println(modelResult);
            }
            System.out.println("=============================");

            modelResults = sqlClient.nativeQuery()
                    .select("select * from demo");

            for (ModelResult modelResult : modelResults) {
                System.out.println(modelResult);
            }

//        sqlClient.model(TestTable.class)
//                .where("name", "name")
//                .delete();
//        sqlClient.close();

            System.out.println(sqlClient.model("test").count());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
