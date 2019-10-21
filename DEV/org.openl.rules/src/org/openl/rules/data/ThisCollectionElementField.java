package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.CollectionType;
import org.openl.vm.IRuntimeEnv;

public class ThisCollectionElementField extends AOpenField {
    private int elementIndex;
    private CollectionType collectionType;
    private Object mapKey;

    public ThisCollectionElementField(int elementIndex, IOpenClass type, CollectionType collectionType) {
        super(getName("", String.valueOf(elementIndex)), type);
        this.elementIndex = elementIndex;
        this.collectionType = collectionType;
    }

    public ThisCollectionElementField(Object mapKey, IOpenClass type) {
        super(getName("", String.valueOf(mapKey)), type);
        this.mapKey = mapKey;
        this.collectionType = CollectionType.MAP;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

        Object res = null;
        if (collectionType.isArray()) {
            res = getForArray(target);
        }
        if (collectionType.isList()) {
            res = getForList(target);
        }
        if (collectionType.isMap()) {
            res = getForMap(target);
        }
        return res != null ? res : getType().nullObject();
    }

    private Object getForList(Object target) {
        @SuppressWarnings("rawtypes")
        List targetList = (List) target;
        if (targetList == null || targetList.size() < elementIndex + 1) {
            return getType().nullObject();
        } else {
            return targetList.get(elementIndex);
        }
    }

    private Object getForArray(Object target) {
        if (target == null || Array.getLength(target) < elementIndex + 1) {
            return getType().nullObject();
        } else {
            return Array.get(target, elementIndex);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getForMap(Object v) {
        Map<Object, Object> map = (Map<Object, Object>) v;
        return map.get(mapKey);
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target == null) {
            throw new OpenLRuntimeException(String.format("Can not set [%s] field to 'null' object", this.getName()));
        }
        if (collectionType.isArray()) {
            setForArray(target, value, env);
        }
        if (collectionType.isList()) {
            setForList(target, value);
        }
        if (collectionType.isMap()) {
            setForMap(target, value);
        }

    }

    @SuppressWarnings("unchecked")
    private void setForList(Object target, Object value) {
        @SuppressWarnings("rawtypes")
        List targetList = (List) target;
        while (targetList.size() <= elementIndex) {
            targetList.add(getType().nullObject());
        }
        targetList.set(elementIndex, value);
    }

    private void setForArray(Object arr, Object value, IRuntimeEnv env) {
        if (Array.getLength(arr) < elementIndex + 1) {
            Object newArray = Array.newInstance(this.getType().getInstanceClass(), elementIndex + 1);

            int oldArryLeng = Array.getLength(arr);
            for (int i = 0; i < oldArryLeng; i++) {
                Array.set(newArray, i, Array.get(arr, i));
            }

            Array.set(newArray, elementIndex, value);
            // Update this variable
            env.popThis();
            env.pushThis(newArray);
        } else {
            Array.set(arr, elementIndex, value);
        }
    }

    @SuppressWarnings("unchecked")
    private void setForMap(Object target, Object value) {
        Map<Object, Object> map = (Map<Object, Object>) target;
        map.put(mapKey, value);
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    private static String getName(String name, String index) {
        return name + "[" + index + "]";
    }

}
