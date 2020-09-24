package test;

import com.loror.sql.ModelResult;
import com.loror.sql.ModelResultList;
import com.loror.sql.SQLClient;
import com.loror.sql.mysql.MySQLClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Test {

    public static void main(String[] args) {

        try (SQLClient sqlClient = new MySQLClient("jdbc:mysql://localhost:3306/test?useOldAliasMetadataBehavior=true", "root", "11231123")) {

            //日志
            sqlClient.setLogListener((connect, sql) -> {
                System.out.println(sql + (connect ? "(查询)" : "(缓存)"));
                singleTable(sql);
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

    private static String singleTable(String baseSql) {
        if (baseSql == null) {
            return null;
        }
        String sql = baseSql.toLowerCase();

        String table = null;

        int index = -1;
        while ((index = sql.indexOf("from ", index + 1)) != -1) {
            int left = index;
            left += 4;
            while (sql.charAt(left) == ' ') {
                left++;
            }

            if (left == sql.length() - 1) {
                break;
            }

            if (sql.charAt(left) == '`') {
                int right = sql.indexOf("`", left + 1);
                if (right != -1) {
                    if (table != null) {
                        table = null;
                        break;
                    }
                    table = sql.substring(left + 1, right);
                }
                continue;
            }

            int right = sql.indexOf(" on", index);
            if (right == -1) {
                right = sql.indexOf(" where", index);
            }
            if (right == -1) {
                right = sql.indexOf(" having", index);
            }
            if (right == -1) {
                right = sql.indexOf(" group by", index);
            }
            if (right == -1) {
                right = sql.indexOf(" limit", index);
            }
            String item;
            if (right != -1) {
                item = sql.substring(left, right);
            } else {
                item = sql.substring(left);
            }
            List<String> names = new ArrayList<>();

            while (true) {
                int itemRight = item.indexOf(" left join");
                if (itemRight != -1) {
                    names.add(item.substring(0, itemRight));
                    item = item.substring(itemRight + 10);
                }
                itemRight = item.indexOf(",");
                if (itemRight != -1) {
                    names.add(item.substring(0, itemRight));
                    item = item.substring(itemRight + 1);
                } else {
                    names.add(item);
                    break;
                }
            }
            for (String name : names) {
                name = name.trim();
                int nameRight = name.indexOf(" as");
                if (nameRight != -1) {
//                    table.add(name.substring(0, nameRight));
                } else {
//                    table.add(name);
                }
            }
        }
        return table;
    }
}
