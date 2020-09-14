package com.loror.sql.mysql;

import com.loror.sql.ModelResult;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class MySQLResult {

    /**
     * 处理查询结果
     */
    public static List<ModelResult> find(ResultSet cursor) {
        List<ModelResult> modelResults = new ArrayList<>();
        if (cursor != null) {
            try {
                List<String> columnNames = new ArrayList<>();
                ResultSetMetaData data = cursor.getMetaData();
                int columnCount = data.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    columnNames.add(data.getColumnName(i + 1));
                }
                while (cursor.next()) {
                    ModelResult modelResult = new ModelResult();
                    modelResults.add(modelResult);
                    for (int i = 0; i < columnCount; i++) {
                        String result = cursor.getString(i + 1);
                        modelResult.set(columnNames.get(i), result);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return modelResults;
    }
}
