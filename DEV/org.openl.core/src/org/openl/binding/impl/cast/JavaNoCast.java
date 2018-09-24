package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public final class JavaNoCast implements IOpenCast {

    private JavaNoCast() {
        // Use JavaNoCast.instance.
    }

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
        return CastFactory.NO_CAST_DISTANCE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    public boolean isImplicit() {
        return true;
    }

    public static IOpenCast instance = new JavaNoCast();
}
