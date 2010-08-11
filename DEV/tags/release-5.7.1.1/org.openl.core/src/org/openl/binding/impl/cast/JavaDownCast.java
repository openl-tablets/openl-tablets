package org.openl.binding.impl.cast;

import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;

public class JavaDownCast implements IOpenCast {

    public Object convert(Object from) {
        return from;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass,
     *      org.openl.types.IOpenClass)
     */
    public int getDistance(IOpenClass from, IOpenClass to) {
        if (from.getInstanceClass() == null) {
            return 0;
        }
        
        return from.getInstanceClass().getSuperclass() == to.getInstanceClass() ? 1 : 2;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    public boolean isImplicit() {
        return true;
    }

}
