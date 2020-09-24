package test;

import com.loror.sql.ModelResult;
import com.loror.sql.ModelResultList;
import com.loror.sql.SQLClient;
import com.loror.sql.mysql.MySQLClient;

import java.util.HashMap;
import java.util.List;

public class Test {

    public static void main(String[] args) {

        try (SQLClient sqlClient = new MySQLClient("jdbc:mysql://localhost:3306/test?useOldAliasMetadataBehavior=true", "root", "11231123")) {

            //日志
            sqlClient.setLogListener((connect, sql) -> {
                System.out.println(sql + (connect ? "(查询)" : "(缓存)"));
            });

            //设置缓存
            HashMap<String, ModelResultList> cache = new HashMap<>();
            sqlClient.setSQLCache(new SQLClient.SQLCache() {

                @Override
                public ModelResultList beforeQuery(SQLClient.QueryIdentification identification) {
                    return cache.get(identification.getSql());
                }

                @Override
                public void onExecuteQuery(SQLClient.QueryIdentification identification, ModelResultList modelResults) {
                    if (modelResults != null) {
                        cache.put(identification.getSql(), modelResults);
                    }
                }

                @Override
                public void onExecute(SQLClient.QueryIdentification identification) {
                    cache.clear();
                }
            });

            //同步表信息
            sqlClient.createTableIfNotExists(TestTable.class);
            sqlClient.changeTableIfColumnAdd(TestTable.class);

            //保存
            sqlClient.model("test")
                    .save(new ModelResult()
                            .add("name", "test")
                            .add("email", "test@qq.com")
                            .add("random", (int) (Math.random() * 100)));

            //native查询
            ModelResultList modelResults = sqlClient.nativeQuery()
                    .executeQuery("select * from test left join demo on test.id = demo.tid");

            System.out.println("=============================");
            for (ModelResult modelResult : modelResults) {
                System.out.println(modelResult);
            }
            System.out.println("=============================");

            //连表查询
            List<Integer> ids = sqlClient.model("test")
                    .join("demo", "test.id = demo.tid")
                    .select("test.id")
                    .where("test.id", "<>", 0)
                    .get()
                    .filter(modelResult -> modelResult.getInt("id", 0));

            System.out.println(ids);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
