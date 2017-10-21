package org.openl.binding.impl.cast;

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
        if (from.getInstanceClass() == null) {
            return 0;
        }

        return CastFactory.JAVA_UP_CAST_DISTANCE;
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
