package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Element in array/list/map field
 *
 * @author PTarasevich
 */

public class CollectionElementField extends AOpenField {
    private final Logger log = LoggerFactory.getLogger(CollectionElementField.class);
    private int elementIndex;
    private Object mapKey;
    private IOpenField field;
    private CollectionType collectionType;
    
    public CollectionElementField(IOpenField field,
            int elementIndex,
            IOpenClass type,
            CollectionType collectionType) {
        super(getName(field.getName(), String.valueOf(elementIndex)), type);
        this.elementIndex = elementIndex;
        this.field = field;
        this.collectionType = collectionType;
    }

    public CollectionElementField(IOpenField field,
            Object mapKey,
            IOpenClass type) {
        super(getName(field.getName(), String.valueOf(mapKey)), type);
        this.mapKey = mapKey;
        this.field = field;
        this.collectionType = CollectionType.MAP;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

        Object res = null;
        Object v = field.get(target, env);
        if (collectionType.isArray()) {
            res = getForArray(v);
        }
        if (collectionType.isList()) {
            res = getForList(v);
        }
        if (collectionType.isMap()) {
            res = getForMap(v);
        }
        return res != null ? res : getType().nullObject();
    }

    private Object getForArray(Object v) {
        if (v == null || Array.getLength(v) < elementIndex + 1) {
            return getType().nullObject();
        } else {
            return Array.get(v, elementIndex);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getForList(Object v) {
        List<Object> list = (List<Object>) v;
        if (list == null || list.size() < elementIndex + 1) {
            return getType().nullObject();
        } else {
            return list.get(elementIndex);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getForMap(Object v) {
        Map<Object, Object> map = (Map<Object, Object>) v;
        if (v == null) {
            return getType().nullObject();
        }
        return map.get(mapKey);
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target == null) {
            throw new OpenLRuntimeException(String.format("Can not set [%s] field to \"null\" object", this.getName()));
        }

        Object v = field.get(target, env);
        if (collectionType.isArray()) {
            setForArray(target, value, env, v);
        }
        if (collectionType.isList()) {
            setForList(target, value, env, v);
        }
        if (collectionType.isMap()) {
            setForMap(target, value, env, v);
        }
    }

    private void setForArray(Object target, Object value, IRuntimeEnv env, Object v) {
        if (v == null) {
            Object array = Array.newInstance(this.getType().getInstanceClass(), elementIndex + 1);
            Array.set(array, elementIndex, value);

            setIntoTarget(target, array, env);
        } else if (Array.getLength(v) < elementIndex + 1) {
            Object newArray = Array.newInstance(this.getType().getInstanceClass(), elementIndex + 1);

            int oldArryLeng = Array.getLength(v);
            for (int i = 0; i < oldArryLeng; i++) {
                Array.set(newArray, i, Array.get(v, i));
            }

            Array.set(newArray, elementIndex, value);
            setIntoTarget(target, newArray, env);
        } else {
            Array.set(v, elementIndex, value);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void setForList(Object target, Object value, IRuntimeEnv env, Object v) {
        if (v == null) {
            List<Object> list = new ArrayList<>();
            while (list.size() <= elementIndex) {
                list.add(getType().nullObject());
            }
            list.set(elementIndex, value);
            setIntoTarget(target, list, env);
        } else {
            List<Object> list = (List<Object>) v; 
            while (list.size() <= elementIndex) {
                list.add(getType().nullObject());
            }
            list.set(elementIndex, value);
        }
    }

    @SuppressWarnings("unchecked")
    private void setForMap(Object target, Object value, IRuntimeEnv env, Object v) {
        if (v == null) {
            Map<Object, Object> map = new HashMap<>();
            map.put(mapKey, value);
            setIntoTarget(target, map, env);
        } else {
            Map<Object, Object> map = (Map<Object, Object>) v; 
            map.put(mapKey, value);
        }
    }

    public boolean isWritable() {
        return true;
    }

    private static String getName(String name, String index) {
        return name + "[" + index + "]";
    }

    private void setIntoTarget(Object target, Object v, IRuntimeEnv env) {
        field.set(target, v, env);
    }

}
