package org.openl.types.impl;

import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;

/**
 * Element in array field
 *
 * @author PTarasevich
 */

public class DatatypeArrayElementField extends AOpenField {
    private final Logger log = LoggerFactory.getLogger(DatatypeOpenField.class);
    private int elementIndex;
    private IOpenField field;

    public DatatypeArrayElementField(IOpenField field, int elementIndex, IOpenClass type) {
        super(getName(field.getName(), elementIndex), type);
        this.elementIndex = elementIndex;
        this.field = field;
    }

    public DatatypeArrayElementField(IOpenField field, int elementIndex) {
        super(getName(field.getName(), elementIndex), field.getType().getComponentClass());
        this.elementIndex = elementIndex;
        this.field = field;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

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
            throw new OpenLRuntimeException(String
                    .format("Can not set [%s] field to \"null\" object", this.getName()));
        }

        try {
            Object arr = field.get(target, env);

            if (arr == null) {
                Object array = Array.newInstance(this.getType().getInstanceClass(), elementIndex + 1);
                Array.set(array, elementIndex, value);

                setArrayIntoTarget(target, array, env);
            } else if (Array.getLength(arr) < elementIndex + 1) {
                Object newArray = Array.newInstance(this.getType().getInstanceClass(), elementIndex + 1);

                int oldArryLeng = Array.getLength(arr);
                for (int i = 0; i < oldArryLeng; i++) {
                    Array.set(newArray, i, Array.get(arr, i));
                }

                Array.set(newArray, elementIndex, value);
                setArrayIntoTarget(target, newArray, env);
            } else {
                Array.set(arr, elementIndex, value);
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

    private static String getName(String name, int index) {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(name).append("[").append(index).append("]");

        return strBuf.toString();
    }

    private void setArrayIntoTarget(Object target, Object array, IRuntimeEnv env) {
        field.set(target, array, env);
    }

}
