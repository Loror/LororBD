package com.loror.sql;

import java.util.ArrayList;
import java.util.List;

public class ModelResultList extends ArrayList<ModelResult> {

    /**
     * 转对象List，@Table对象按照@Column赋值；普通对象按照变量名赋值
     */
    public <T> List<T> list(Class<T> type) {
        List<T> list = new ArrayList<>();
        for (ModelResult modelResult : this) {
            list.add(modelResult.object(type));
        }
        return list;
    }

    public void forEach(OnForEach onForEach) {
        if (onForEach != null) {
            for (ModelResult result : this) {
                onForEach.item(result);
            }
        }
    }

    public <T> List<T> filter(OnFilter<T> filter) {
        List<T> list = new ArrayList<>();
        if (filter != null) {
            for (ModelResult result : this) {
                list.add(filter.item(result));
            }
        }
        return list;
    }

    public interface OnFilter<T> {
        T item(ModelResult modelResult);
    }

    public interface OnForEach {
        void item(ModelResult modelResult);
    }
}
