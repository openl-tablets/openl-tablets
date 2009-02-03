/**
 * 
 */
package org.openl.types.impl;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;

/**
 * @author User
 *
 */
public class DelegatedDynamicObject extends DynamicObject {

    protected IDynamicObject parent;
    
    /**
     * @param type
     */
    public DelegatedDynamicObject(IOpenClass type, IDynamicObject parent) {
        super(type);
        this.parent = parent;
    }

    /* (non-Javadoc)
     * @see org.openl.types.impl.DynamicObject#getFieldValue(java.lang.String)
     */
    @Override
    public Object getFieldValue(String name) {
        if (isMyField(name)){
            return super.getFieldValue(name);
        } else {
            return parent.getFieldValue(name);
        }
    }

    /* (non-Javadoc)
     * @see org.openl.types.impl.DynamicObject#setFieldValue(java.lang.String, java.lang.Object)
     */
    @Override
    public void setFieldValue(String name, Object value) {
        if (isMyField(name)){
            super.setFieldValue(name, value);
        } else {
            parent.setFieldValue(name, value);
        }
    }
    
    

}
