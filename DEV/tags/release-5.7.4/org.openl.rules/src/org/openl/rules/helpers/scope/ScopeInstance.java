package org.openl.rules.helpers.scope;

import org.openl.types.impl.DynamicObject;

/**
 * @deprecated 12.11.2010 what is it for?
 * @author DLiauchuk
 *
 */
@Deprecated
public class ScopeInstance extends DynamicObject {

    private DynamicObject parent;

    public ScopeInstance(Scope scope) {
        super(scope);
        // this.parent = parent;
    }

    @Override
    public Object getFieldValue(String name) {
        if (isMyField(name)) {
            return super.getFieldValue(name);
        }

        return parent.getFieldValue(name);
    }

    public Scope getScope() {
        return (Scope) getType();
    }

    @Override
    public void setFieldValue(String name, Object value) {
        if (isMyField(name)) {
            super.setFieldValue(name, value);
            return;
        }

        parent.setFieldValue(name, value);
    }

}
