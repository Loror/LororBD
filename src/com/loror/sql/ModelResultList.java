package com.loror.sql;

import java.util.ArrayList;
import java.util.List;

public class ModelResultList extends ArrayList<ModelResult> {

    public <T> List<T> list(Class<T> type) {
        List<T> list = new ArrayList<>();
        for (ModelResult modelResult : this) {
            list.add(modelResult.object(type));
        }
        return list;
    }
}
