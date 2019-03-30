package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.CollectionType;
import org.openl.vm.IRuntimeEnv;

public class CollectionElementWithMultiRowField extends AOpenField {
    private IOpenField field;
    private String fieldPathFromRoot;
    private boolean pkField = false;
    private CollectionType collectionType;

    public CollectionElementWithMultiRowField(IOpenField field,
            String fieldPathFromRoot,
            IOpenClass type,
            CollectionType collectionType) {
        this(field, fieldPathFromRoot, type, collectionType, false);
    }

    public CollectionElementWithMultiRowField(IOpenField field,
            String fieldPathFromRoot,
            IOpenClass type,
            CollectionType collectionType,
            boolean pkField) {
        super(getName(field.getName()), type);
        this.field = field;
        this.pkField = pkField;
        this.fieldPathFromRoot = fieldPathFromRoot;
        this.collectionType = collectionType;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

        DatatypeArrayMultiRowElementContext context = (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];
        int elementIndex = context.getIndex(fieldPathFromRoot, target);

        Object res = null;
        Object v = field.get(target, env);
        if (collectionType.isArray()) {
            res = getForArray(elementIndex, v);
        }
        if (collectionType.isList()) {
            res = getForList(elementIndex, v);
        }
        return res != null ? res : getType().nullObject();
    }

    private Object getForArray(int elementIndex, Object arr) {
        if (arr == null || Array.getLength(arr) < elementIndex + 1) {
            return getType().nullObject();
        } else {
            return Array.get(arr, elementIndex);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getForList(int elementIndex, Object v) {
        List<Object> list = (List<Object>) v;
        if (v == null || list.size() < elementIndex + 1) {
            return getType().nullObject();
        } else {
            return list.get(elementIndex);
        }
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target == null) {
            throw new OpenLRuntimeException(String.format("Can not set [%s] field to \"null\" object", this.getName()));
        }

        Object v = field.get(target, env);
        DatatypeArrayMultiRowElementContext context = (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];
        int elementIndex = context.getIndex(fieldPathFromRoot, target);
        if (collectionType.isArray()) {
            setForArray(target, value, env, v, elementIndex);
        }
        if (collectionType.isList()) {
            setForList(target, value, env, v, elementIndex);
        }

    }

    private void setForArray(Object target, Object value, IRuntimeEnv env, Object v, int elementIndex) {
        if (v == null) {
            if (!isPkField()) {
                Object array = Array.newInstance(this.getType().getInstanceClass(), 1);
                Array.set(array, 0, value);
                setIntoTarget(target, array, env);
            }
        } else {
            if (Array.getLength(v) < elementIndex + 1) {
                Object newArray = Array.newInstance(this.getType().getInstanceClass(), elementIndex + 1);

                int oldArryLeng = Array.getLength(v);
                for (int i = 0; i < oldArryLeng; i++) {
                    Array.set(newArray, i, Array.get(v, i));
                }
                if (!isPkField()) {
                    Array.set(newArray, elementIndex, value);
                    setIntoTarget(target, newArray, env);
                }
            } else {
                if (!isPkField()) {
                    Array.set(v, elementIndex, value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setForList(Object target, Object value, IRuntimeEnv env, Object v, int elementIndex) {
        if (v == null) {
            if (!isPkField()) {
                List<Object> list = new ArrayList<>();
                while (list.size() <= elementIndex) {
                    list.add(getType().nullObject());
                }
                list.set(elementIndex, value);
                setIntoTarget(target, list, env);
            }
        } else {
            List<Object> list = (List<Object>) v;
            while (list.size() <= elementIndex) {
                list.add(getType().nullObject());
            }
            if (!isPkField()) {
                list.set(elementIndex, value);
            }
        }
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    public String getFieldPathFromRoot() {
        return fieldPathFromRoot;
    }

    private static String getName(String name) {
        return name + "[]";
    }

    public IOpenField getField() {
        return field;
    }

    private void setIntoTarget(Object target, Object array, IRuntimeEnv env) {
        field.set(target, array, env);
    }

    public boolean isPkField() {
        return pkField;
    }

    public CollectionType getCollectionType() {
        return collectionType;
    }

}
