package com.loror.sql;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModelResult {

    private Map<String, String> data = new HashMap<>();

    public Set<String> keys() {
        return data.keySet();
    }

    public void set(String name, String value) {
        if (name == null) {
            return;
        }
        data.put(name, value);
    }

    public String get(String name) {
        if (name == null) {
            return null;
        }
        return data.get(name);
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
        if (type == null) {
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
            for (String name : data.keySet()) {
                String value = data.get(name);
                Field field;
                try {
                    field = type.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    continue;
                }
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (value != null) {
                    try {
                        if (fieldType == int.class || fieldType == Integer.class) {
                            field.set(entity, Integer.parseInt(value));
                        } else if (fieldType == long.class || fieldType == Long.class) {
                            field.set(entity, Long.parseLong(value));
                        } else if (fieldType == float.class || fieldType == Float.class) {
                            field.set(entity, Float.parseFloat(value));
                        } else if (fieldType == double.class || fieldType == Double.class) {
                            field.set(entity, Double.parseDouble(value));
                        } else if (fieldType == String.class) {
                            field.set(entity, value);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
        for (ModelInfo.ColumnInfo columnInfo : modelInfo.getColumnInfos()) {
            String value = data.get(columnInfo.getName());
            Field field = columnInfo.getField();
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            if (value != null) {
                try {
                    if (fieldType == int.class || fieldType == Integer.class) {
                        field.set(entity, Integer.parseInt(value));
                    } else if (fieldType == long.class || fieldType == Long.class) {
                        field.set(entity, Long.parseLong(value));
                    } else if (fieldType == float.class || fieldType == Float.class) {
                        field.set(entity, Float.parseFloat(value));
                    } else if (fieldType == double.class || fieldType == Double.class) {
                        field.set(entity, Double.parseDouble(value));
                    } else if (fieldType == String.class) {
                        field.set(entity, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return entity;
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
