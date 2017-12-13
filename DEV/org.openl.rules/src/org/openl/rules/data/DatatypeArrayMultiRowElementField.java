package org.openl.rules.data;

import java.lang.reflect.Array;

import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatatypeArrayMultiRowElementField extends AOpenField {
    private final Logger log = LoggerFactory.getLogger(DatatypeArrayMultiRowElementField.class);
    private IOpenField field;
    private String fieldPathFromRoot;
    private boolean pkField = false;

    public DatatypeArrayMultiRowElementField(IOpenField field, String fieldPathFromRoot, IOpenClass type) {
        super(getName(field.getName()), type);
        this.field = field;
        this.fieldPathFromRoot = fieldPathFromRoot;
    }
    
    public DatatypeArrayMultiRowElementField(IOpenField field, String fieldPathFromRoot, IOpenClass type, boolean pkField) {
        super(getName(field.getName()), type);
        this.field = field;
        this.pkField = pkField;
        this.fieldPathFromRoot = fieldPathFromRoot;
    }

    public DatatypeArrayMultiRowElementField(IOpenField field, String fieldPathFromRoot) {
        super(getName(field.getName()), field.getType().getComponentClass());
        this.field = field;
        this.fieldPathFromRoot = fieldPathFromRoot;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

        DatatypeArrayMultiRowElementContext context = (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];

        int elementIndex = context.getIndex(fieldPathFromRoot, target);

        Object res = null;
        try {
            Object arr = field.get(target, env);

            if (arr == null || Array.getLength(arr) < elementIndex + 1) {
                res = getType().nullObject();
            } else {
                res = Array.get(arr, elementIndex);
            }
        } catch (SecurityException e) {
            processError(e);
        }
        return res != null ? res : getType().nullObject();
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target == null) {
            throw new OpenLRuntimeException(String.format("Can not set [%s] field to \"null\" object", this.getName()));
        }

        try {
            Object arr = field.get(target, env);
            DatatypeArrayMultiRowElementContext context = (DatatypeArrayMultiRowElementContext) env
                    .getLocalFrame()[0];
            int elementIndex = context.getIndex(fieldPathFromRoot, target);
            if (arr == null) {
                if (!isPkField()) {
                    Object array = Array.newInstance(this.getType().getInstanceClass(), 1);
                    Array.set(array, 0, value);
                    setArrayIntoTarget(target, array, env);
                }
            } else {
                if (Array.getLength(arr) < elementIndex + 1) {
                    Object newArray = Array.newInstance(this.getType().getInstanceClass(), elementIndex + 1);

                    int oldArryLeng = Array.getLength(arr);
                    for (int i = 0; i < oldArryLeng; i++) {
                        Array.set(newArray, i, Array.get(arr, i));
                    }
                    if (!isPkField()) {
                        Array.set(newArray, elementIndex, value);
                        setArrayIntoTarget(target, newArray, env);
                    }
                } else {
                    if (!isPkField()) {
                        Array.set(arr, elementIndex, value);
                    }
                }
            }
        } catch (SecurityException e) {
            processError(e);
        }

    }

    private void processError(Throwable e1) {
        log.error("{}", this, e1);
        OpenLMessagesUtils.addError(e1);
    }

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

    private void setArrayIntoTarget(Object target, Object array, IRuntimeEnv env) {
        field.set(target, array, env);
    }
    
    public boolean isPkField() {
        return pkField;
    }

}
