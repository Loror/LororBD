package com.loror.sql;

import java.io.Closeable;
import java.util.List;

public interface SQLClient extends Closeable {

    interface OnClose {
        void close(SQLClient sqlClient);
    }

    interface LogListener {
        void log(boolean connect, String sql);
    }

    interface SQLCache {
        ModelResultList beforeQuery(QueryIdentification identification);

        void onExecuteQuery(QueryIdentification identification, ModelResultList modelResults);

        void onExecute(QueryIdentification identification);
    }

    class QueryIdentification {

        private String sql;
        private boolean isNative;
        private String model;
        private String select;
        private List<Join> joins;
        private ConditionRequest conditionRequest;

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public boolean isNative() {
            return isNative;
        }

        public void setNative(boolean aNative) {
            isNative = aNative;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getSelect() {
            return select;
        }

        public void setSelect(String select) {
            this.select = select;
        }

        public List<Join> getJoins() {
            return joins;
        }

        public void setJoins(List<Join> joins) {
            this.joins = joins;
        }

        public ConditionRequest getConditionRequest() {
            return conditionRequest;
        }

        public void setConditionRequest(ConditionRequest conditionRequest) {
            this.conditionRequest = conditionRequest;
        }
    }

    /**
     * 重新开启
     */
    void reStart();

    /**
     * 关闭
     */
    void close();

    /**
     * 是否关闭
     */
    boolean isClosed();

    /**
     * 代理close方法
     */
    void setOnClose(OnClose onClose);

    /**
     * 代理log
     */
    void setLogListener(LogListener logListener);

    /**
     * 设置sql缓存
     */
    void setSQLCache(SQLCache sqlCache);

    /**
     * 创建表
     */
    void createTableIfNotExists(Class<?> table);

    /**
     * 删除表
     */
    void dropTable(Class<?> table);

    /**
     * 表字段增加
     * 注意：执行更新表后，可能表未及时变化，下次执行会再次执行更新表而报错，需保证close之前只执行一次
     */
    void changeTableIfColumnAdd(Class<?> table);

    /**
     * 获取model信息
     */
    ModelInfo getModel(Class<?> table);

    /**
     * 获取条件处理model
     */
    Model model(String table);

    /**
     * 事务
     */
    boolean transaction(Runnable runnable);

    /**
     * 获取原生执行
     */
    SQLDataBase nativeQuery();

}
