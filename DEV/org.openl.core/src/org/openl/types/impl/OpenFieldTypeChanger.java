/*
 * Created on Jul 1, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class OpenFieldTypeChanger extends OpenFieldDelegator {

    protected IOpenClass changedClass;

    protected ITypeConvertor adaptor;

    /**
     * @param field
     */
    public OpenFieldTypeChanger(IOpenField field, IOpenClass changedClass, ITypeConvertor adaptor) {
        super(field);

        this.changedClass = changedClass;
        this.adaptor = adaptor;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#get(java.lang.Object)
     */
    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return adaptor.srcToDest(field.get(target, env));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getType()
     */
    @Override
    public IOpenClass getType() {
        return changedClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenField#set(java.lang.Object, java.lang.Object)
     */
    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        field.set(target, adaptor.destToSrc(value), env);
    }

}
