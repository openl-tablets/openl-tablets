package org.openl.binding.impl.cast;

import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;

public class JavaUpCast implements IOpenCast {

    public Object convert(Object from) {
        return from;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass,
     * org.openl.types.IOpenClass)
     */
    public int getDistance(IOpenClass from, IOpenClass to) {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    public boolean isImplicit() {
        return false;
    }

}
