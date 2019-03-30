package org.openl.binding.impl.cast;

import org.openl.binding.ICastFactory;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

final class JavaDownCast implements IOpenCast {
    
    private IOpenClass to;
    private ICastFactory castFactory;

    JavaDownCast(IOpenClass to, ICastFactory castFactory) {
        if (to == null) {
            throw new IllegalArgumentException("to arg can't be null!");
        }
        if (castFactory == null){
            throw new IllegalArgumentException("castFactory arg can't be null!");
        }
        this.to = to;
        this.castFactory = castFactory;
    }

    @Override
    public Object convert(Object from) {
        if (from == null){
            return null;
        }
        if (from.getClass().isAssignableFrom(to.getInstanceClass())) {
            return from;
        } else {
            //Allow upcast if posible
            if (to.getInstanceClass().isAssignableFrom(from.getClass())) {
                return from;    
            }else{
                IOpenCast openCast = castFactory.getCast(JavaOpenClass.getOpenClass(from.getClass()), to);
                if (openCast != null && !(openCast instanceof JavaDownCast)){
                    return openCast.convert(from);
                }
                throw new ClassCastException("Can't cast from '" + from.getClass().getCanonicalName() + "' to " + to.getDisplayName(0));
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass,
     * org.openl.types.IOpenClass)
     */
    @Override
    public int getDistance() {
        return CastFactory.JAVA_DOWN_CAST_DISTANCE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenCast#isImplicit()
     */
    @Override
    public boolean isImplicit() {
        return false;
    }

}
