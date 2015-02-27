package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class JavaDownCast implements IOpenCast {
    
    private IOpenClass to;

    public JavaDownCast(IOpenClass to) {
        if (to == null) {
            throw new IllegalArgumentException("to arg can't be null!");
        }
        this.to = to;
    }

    public Object convert(Object from) {
        if (from == null){
            return null;
        }
        if (from.getClass().isAssignableFrom(to.getInstanceClass())) {
            return from;
        } else {
            throw new ClassCastException("Can't cast from '" + from.getClass().getCanonicalName() + "' to " + to.getDisplayName(0));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass,
     * org.openl.types.IOpenClass)
     */
    public int getDistance(IOpenClass from, IOpenClass to) {
        return 9;
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
