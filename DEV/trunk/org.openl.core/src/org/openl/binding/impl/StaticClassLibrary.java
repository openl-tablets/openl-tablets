/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.Iterator;

import org.openl.binding.IOpenLibrary;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.util.AOpenIterator;
import org.openl.util.ASelector;
import org.openl.util.ISelector;

/**
 * @author snshor
 *
 */
public class StaticClassLibrary implements IOpenLibrary {

    private IOpenClass openClass;

    public StaticClassLibrary() {
    }

    public StaticClassLibrary(IOpenClass openClass) {
        this.openClass = openClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String,
     *      java.lang.String, org.openl.types.IOpenClass[])
     */
    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params) {
        return openClass.getMethod(name, params);
    }

    public IOpenField getVar(String name, boolean strictMatch) {
        return openClass.getField(name, strictMatch);
    }

    public Iterator<IOpenMethod> methods() {
        ISelector<IOpenMethod> sel = new ASelector<IOpenMethod>() {
            // TODO fix if necessary
            @Override
            public int redefinedHashCode() {
                return "static".hashCode();
            }

            public boolean select(IOpenMethod m) {
                return m.isStatic();
            }
        };

        return AOpenIterator.select(openClass.methods(), sel);
    }

    public void setOpenClass(IOpenClass c) {
        openClass = c;
    }

}
