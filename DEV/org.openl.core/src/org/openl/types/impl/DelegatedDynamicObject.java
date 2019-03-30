/**
 *
 */
package org.openl.types.impl;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;

/**
 * The <code>DelegatedDynamicObject</code> class wraps other <code>IDynamicObject</code> class and serves as aggregation
 * of both itself and other class. So the consumer code can access both its fields and wrapped class fields without a
 * notice.
 */
public class DelegatedDynamicObject extends DynamicObject {

    private IDynamicObject parent;

    /**
     * Create an instance by its type and other instance to be wrapped. All the fields of other
     * <code>IDynamicObject</code> instance will be accessible through this instance.
     *
     * @param type The type of the instance
     * @param parent The other instance to wrap
     */
    public DelegatedDynamicObject(IOpenClass type, IDynamicObject parent) {
        super(type);
        this.parent = parent;
    }

    @Override
    public Object getFieldValue(String name) {
        Object value;

        if (isMyField(name) || getFieldValues().containsKey(name)) {
            value = super.getFieldValue(name);
        } else {
            value = parent.getFieldValue(name);
        }

        return value;
    }

    @Override
    public void setFieldValue(String name, Object value) {
        if (isMyField(name) || getFieldValues().containsKey(name)) {
            super.setFieldValue(name, value);
        } else {
            parent.setFieldValue(name, value);
        }
    }

    public Object getFieldValue(String name, boolean ignoreDelegate) {
        if (ignoreDelegate) {
            return super.getFieldValue(name);
        } else {
            return this.getFieldValue(name);
        }
    }

    public void setFieldValue(String name, Object value, boolean ignoreDeligate) {
        if (ignoreDeligate) {
            super.setFieldValue(name, value);
        } else {
            this.setFieldValue(name, value);
        }
    }

    public boolean isAssignableFrom(Class<?> type) {
        return type.isInstance(parent);
    }

}
