package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class ThrowableVoidCast implements IOpenCast {

    public static final int UP_CAST_DISTANCE = 7;

    public Object convert(Object from) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass,
     * org.openl.types.IOpenClass)
     */


    public int getDistance(IOpenClass from, IOpenClass to) {
        if (from.getInstanceClass() == null) {
            return 0;
        }

        return UP_CAST_DISTANCE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    public boolean isImplicit() {
        return true;
    }
    
    public static final class ThrowableVoid{
        
    }

}
