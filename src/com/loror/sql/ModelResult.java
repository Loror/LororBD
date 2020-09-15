package com.loror.sql;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class ModelResult {

    /**
     * 保证map有序且可重复
     */
    private static class IdentityString {
        private final String value;

        private IdentityString(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IdentityString that = (IdentityString) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private final Map<IdentityString, String> data = new LinkedHashMap<>();

    public List<String> keys() {
        Set<IdentityString> sets = data.keySet();
        List<String> keys = new ArrayList<>(sets.size());
        for (IdentityString set : sets) {
            keys.add(set.value);
        }
        return keys;
    }

    public String intern(String key) {
        if (key == null) {
            return null;
        }
        for (IdentityString identityString : data.keySet()) {
            if (key.equals(identityString.value)) {
                return identityString.value;
            }
        }
        return null;
    }

    public void set(String name, String value) {
        if (name == null) {
            return;
        }
        data.put(new IdentityString(name), value);
    }

    public String get(String name) {
        if (name == null) {
            return null;
        }
        return data.get(new IdentityString(name));
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
            for (IdentityString key : data.keySet()) {
                String value = data.get(key);
                Field field;
                try {
                    field = type.getDeclaredField(key.value);
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
        List<IdentityString> keys = new LinkedList<>(data.keySet());
        for (ModelInfo.ColumnInfo columnInfo : modelInfo.getColumnInfos()) {
            IdentityString key = null;
            Iterator<IdentityString> iterator = keys.iterator();
            while (iterator.hasNext()) {
                IdentityString item = iterator.next();
                if (item.value.equals(columnInfo.getName())) {
                    key = item;
                    iterator.remove();
                    break;
                }
            }
            String value = data.get(key);
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
