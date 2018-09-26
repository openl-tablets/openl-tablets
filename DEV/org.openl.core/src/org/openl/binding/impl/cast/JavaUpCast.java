package org.openl.binding.impl.cast;

public class JavaUpCast implements IOpenCast {

    private int upCastDistance;

    public JavaUpCast(int castDistance) {
        this.upCastDistance = castDistance;
    }

    public Object convert(Object from) {
        return from;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass, org.openl.types.IOpenClass)
     */
    public int getDistance() {
        return upCastDistance;
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
