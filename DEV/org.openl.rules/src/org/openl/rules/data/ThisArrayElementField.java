package org.openl.rules.data;

import java.lang.reflect.Array;

import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThisArrayElementField extends AOpenField {
    private final Logger log = LoggerFactory.getLogger(ThisArrayElementField.class);
    private int elementIndex;

    public ThisArrayElementField(int elementIndex, IOpenClass type) {
        super(getName("", elementIndex), type);
        this.elementIndex = elementIndex;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

        Object res = null;
        try {
            if (target == null || Array.getLength(target) < elementIndex + 1) {
                res = getType().nullObject();
            } else {
                res = Array.get(target, elementIndex);
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
            Object arr = target;
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
        return name + "[" + index + "]";
    }

    public static final class TargetWrapperForSetterMethod {
        private Object target;

        public TargetWrapperForSetterMethod(Object target) {
            this.target = target;
        }

        public Object getTarget() {
            return target;
        }

        public void updateTarget(Object target) {
            this.target = target;
        }
    }
}
