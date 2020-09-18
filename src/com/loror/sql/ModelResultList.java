package com.loror.sql;

import java.util.ArrayList;
import java.util.List;

public class ModelResultList extends ArrayList<ModelResult> {

    public interface OnFilter<T> {
        T item(ModelResult modelResult);
    }

    public <T> List<T> list(Class<T> type) {
        List<T> list = new ArrayList<>();
        for (ModelResult modelResult : this) {
            list.add(modelResult.object(type));
        }
        return list;
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
}
