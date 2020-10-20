package com.loror.sql.mysql;

import com.loror.sql.ModelData;
import com.loror.sql.ModelDataList;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class MySQLResult {

    /**
     * 处理查询结果
     */
    public static ModelDataList find(ResultSet cursor) {
        ModelDataList modelResults = new ModelDataList();
        if (cursor != null) {
            try {
                List<String> columnNames = new ArrayList<>();
                ResultSetMetaData data = cursor.getMetaData();
                int columnCount = data.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    columnNames.add(data.getColumnName(i + 1));
                }
                while (cursor.next()) {
                    ModelData modelResult = new ModelData();
                    modelResults.add(modelResult);
                    for (int i = 0; i < columnCount; i++) {
                        String result = cursor.getString(i + 1);
                        modelResult.add(columnNames.get(i), result);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return modelResults;
    }
}
