package test;

import com.loror.sql.ModelResult;
import com.loror.sql.ModelResultList;
import com.loror.sql.SQLClient;
import com.loror.sql.mysql.MySQLClient;

public class Test {

    public static void main(String[] args) {

        try (SQLClient sqlClient = new MySQLClient("jdbc:mysql://localhost:3306/test?useOldAliasMetadataBehavior=true", "root", "11231123")) {

//            ModelResultList modelResults = sqlClient.model("test")
//                    .select("sum(id)")
//                    .where("id", "<>", 0)
//                    .groupBy("`group`")
//                    .having("sum(id)", ">", 1)
//                    .page(1, 2)
//                    .get();

            ModelResultList modelResults = sqlClient.model("test left join demo on test.id = demo.tid")
//                    .select("test.name as name1,demo.name as name2")
                    .get();

            System.out.println("=============================");
            for (ModelResult modelResult : modelResults) {
                System.out.println(modelResult);
            }
            System.out.println("=============================");

            modelResults = sqlClient.nativeQuery()
                    .select("select * from test");

            for (ModelResult modelResult : modelResults) {
                System.out.println(modelResult.object(TestTable.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
