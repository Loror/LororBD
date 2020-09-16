package com.loror.sql;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class ModelResult {

    /**
     * 保证data有序且可重复
     */
    protected static class IdentityNode {
        private final String key;
        private final String value;

        private IdentityNode(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    private final boolean isNull;

    public ModelResult() {
        this(false);
    }

    public ModelResult(boolean isNull) {
        this.isNull = isNull;
    }

    public boolean isNull() {
        return isNull;
    }

    public interface OnForEach {
        void item(String key, String value);
    }

    private final List<IdentityNode> data = new LinkedList<>();

    public void forEach(OnForEach onForEach) {
        if (isNull) {
            throw new NullPointerException("this result is null");
        }
        if (onForEach != null) {
            for (IdentityNode node : data) {
                onForEach.item(node.key, node.value);
            }
        }
    }

    public List<String> keys() {
        if (isNull) {
            throw new NullPointerException("this result is null");
        }
        List<String> keys = new ArrayList<>(data.size());
        for (IdentityNode item : data) {
            keys.add(item.key);
        }
        return keys;
    }

    public List<String> values(String name) {
        if (isNull) {
            throw new NullPointerException("this result is null");
        }
        List<String> values = new ArrayList<>(data.size());
        if (name != null) {
            for (IdentityNode item : data) {
                if (name.equals(item.key)) {
                    values.add(item.value);
                }
            }
        }
        return values;
    }

    public void addAll(ModelResult modelResult) {
        if (isNull) {
            throw new NullPointerException("this result is null");
        }
        if (modelResult != null) {
            data.addAll(modelResult.data);
        }
    }

    public void add(String name, String value) {
        if (isNull) {
            throw new NullPointerException("this result is null");
        }
        if (name != null) {
            data.add(new IdentityNode(name, value));
        }
    }

    public String get(String name) {
        if (isNull) {
            throw new NullPointerException("this result is null");
        }
        if (name != null) {
            for (IdentityNode item : data) {
                if (name.equals(item.key)) {
                    return item.value;
                }
            }
        }
        return null;
    }

    public int getInt(String name, int defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public long getLong(String name, long defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : Long.parseLong(value);
    }

    private Object getObject(Class<?> type) throws Exception {
        try {
            return type.newInstance();
        } catch (Exception e) {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
    }

    public <T> T object(Class<T> type) {
        if (type == null || isNull) {
            return null;
        }
        T entity;

        if (type.getAnnotation(Table.class) != null) {
            return (T) object(ModelInfo.of(type));
        }

        try {
            entity = (T) getObject(type);
        } catch (Exception e) {
            throw new IllegalArgumentException(type.getSimpleName() + " have no non parametric constructor");
        }
        Field[] fields = type.getDeclaredFields();
        if (fields.length != 0) {
            for (IdentityNode item : data) {
                Field field;
                try {
                    field = type.getDeclaredField(item.key);
                } catch (NoSuchFieldException e) {
                    continue;
                }
                String value = item.value;
                field.setAccessible(true);
                setField(entity, field, value);
            }
        }
        return entity;
    }

    private Object object(ModelInfo modelInfo) {
        if (modelInfo == null) {
            return null;
        }
        Object entity;
        try {
            entity = modelInfo.getTableObject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(modelInfo.getTableName() + " have no non parametric constructor");
        }
        List<IdentityNode> nodes = new LinkedList<>(data);
        for (ModelInfo.ColumnInfo columnInfo : modelInfo.getColumnInfos()) {
            IdentityNode node = null;
            Iterator<IdentityNode> iterator = nodes.iterator();
            while (iterator.hasNext()) {
                IdentityNode item = iterator.next();
                if (item.key.equals(columnInfo.getName())) {
                    node = item;
                    iterator.remove();
                    break;
                }
            }
            if (node == null) {
                continue;
            }
            String value = node.value;
            Field field = columnInfo.getField();
            field.setAccessible(true);
            setField(entity, field, value);
        }
        return entity;
    }

    /**
     * field设置值
     */
    private void setField(Object obj, Field field, String value) {
        if (value != null) {
            Class<?> fieldType = field.getType();
            try {
                if (fieldType == int.class || fieldType == Integer.class) {
                    field.set(obj, Integer.parseInt(value));
                } else if (fieldType == long.class || fieldType == Long.class) {
                    field.set(obj, Long.parseLong(value));
                } else if (fieldType == float.class || fieldType == Float.class) {
                    field.set(obj, Float.parseFloat(value));
                } else if (fieldType == double.class || fieldType == Double.class) {
                    field.set(obj, Double.parseDouble(value));
                } else if (fieldType == String.class) {
                    field.set(obj, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
