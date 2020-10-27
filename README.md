# 数据库访问框架

[![License](https://img.shields.io/badge/License%20-Apache%202-337ab7.svg)](https://www.apache.org/licenses/LICENSE-2.0)

实例代码
```
try (SQLClient sqlClient = new MySQLClient("jdbc:mysql://localhost:3306/test?useOldAliasMetadataBehavior=true", "root", "11231123")) {

            //日志
            sqlClient.setLogListener((connect, sql) -> {
                System.out.println(sql + (connect ? "(查询)" : "(缓存)"));
            });

            //设置缓存
            HashMap<String, ModelDataList> cache = new HashMap<>();
            sqlClient.setSQLCache(new SQLClient.SQLCache() {

                @Override
                public ModelDataList beforeQuery(SQLClient.QueryIdentification identification) {
                    return cache.get(identification.getSql());
                }

                @Override
                public void onExecuteQuery(SQLClient.QueryIdentification identification, ModelDataList modelResults) {
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
                    .save(new ModelData()
                            .add("name", "test")
                            .add("email", "test@qq.com")
                            .add("random", (int) (Math.random() * 100)));

            //native查询
            ModelDataList modelResults = sqlClient.nativeQuery()
                    .executeQuery("select * from test left join demo on test.id = demo.tid");

            System.out.println("=============================");
            for (ModelData modelResult : modelResults) {
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
```

* 该框架支持创建model对象操作，通过@Table，@Id，@Column等注解指定表及字段，也支持直接使用ModelData，ModelDataList操作数据。


## mysql包

* 类MySQLClient
    * 构造方法MySQLClient(String url, String name, String password) 获取MySQLClient对象

## sql包

* 接口SQLClient
    * 接口提供了获取条件查询对象Model，native查询对象SQLDataBase，事务，各种监听，关闭连接等功能。
    
* 类Model
    * 方法where(String key, Object var) 添加and条件
    * 方法where(String key, String operation, Object var) 添加指定operation的条件
    * 方法whereOr(String key, Object var) 添加or条件
    * 方法whereOr(String key, String operation, Object var) 添加指定operation的or条件
    * 方法whereIn(String key, Object[] vars) 添加in条件
    * 方法whereIn(String key, String operation, Object[] var) 添加指定operation（仅支持in,not in）的in条件
    * 方法where(OnWhere onWhere) 添加同级and条件，通过onWhere构造的多个条件与外部同级
    * 方法whereOr(OnWhere onWhere) 添加同级or条件，通过onWhere构造的多个条件与外部同级
    * 方法groupBy(String key) 添加groupBy条件
    * 方法having(String key, String operation, Object var) 添加having条件，有group时方生效
    * 方法orderBy(String key, int order) 添加排序    
    * 方法page(int page, int size) 添加分页
    * 方法join(String model, String on) 连表，同理有leftJoin，rightJoin，innerJoin方法
    * 方法select(String column) 指定查询项，未指定以*查询
    * 方法save(Object entity) 保存数据
    * 方法delete() 删除数据
    * 方法update(ModelData values) 更新数据
    * 方法count() 查询数量
    * 方法get() 查询数据
    * 方法first() 查询首条数据
    
* 类ModelData
    * 该类为链表结构，存储的键值对有序且可重复
    * 方法isNull() 查询数据为空时，该方法返回true
    * 方法forEach(OnForEach onForEach) 遍历所有字段
    * 方法object(Class<T> type) 获取为Model对象，如果是@Table修饰的类将通过注解情况转换，普通对象则通过变量名/类型转换
    * 方法getString(String name) 获取一个字段，返回为String类型
    
* 类ModelDataList
    * 方法list(Class<T> type) 转换为List对象，每一项将调用ModelData的object方法生成对象
    * 方法forEach(OnForEach onForEach) 遍历所有项
    * 方法filter(OnFilter<T> filter) 字段筛选，返回为List
    
License
-------

    Copyright 2018 Loror

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.