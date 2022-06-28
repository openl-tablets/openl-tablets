package org.openl.binding.impl.cast;

import java.lang.reflect.Array;
import java.util.Objects;

import org.openl.binding.ICastFactory;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

final class JavaDownCast implements IOpenCast {

    private final IOpenClass to;
    private final ICastFactory castFactory;

    JavaDownCast(IOpenClass to, ICastFactory castFactory) {
        this.to = Objects.requireNonNull(to, "to cannot be null");
        this.castFactory = Objects.requireNonNull(castFactory, "castFactory cannot be null");
    }

    private int calcArrayMinDim(Object from) {
        if (from != null && from.getClass().isArray()) {
            int arrayLength = Array.getLength(from);
            if (arrayLength > 0) {
                int minDim = Integer.MAX_VALUE;
                for (int i = 0; i < arrayLength; i++) {
                    int p = calcArrayMinDim(Array.get(from, i));
                    if (minDim > p) {
                        minDim = p;
                    }
                }
                return minDim + 1;
            }
        }
        return 0;
    }

    @Override
    public Object convert(Object from) {
        if (from == null) {
            return null;
        }
        if (to.getInstanceClass().isAssignableFrom(from.getClass())) {
            return from;
        } else {
            Class<?> fromClass = from.getClass();
            IOpenClass fromOpenClass = JavaOpenClass.getOpenClass(fromClass);
            IOpenClass t = fromOpenClass;
            int d = 0;
            while (t.isArray()) {
                t = t.getComponentClass();
                d++;
            }
            if (d > 0 && t.getInstanceClass() == Object.class) {
                int dim = calcArrayMinDim(from);
                fromOpenClass = JavaOpenClass.OBJECT.getArrayType(dim);
            }
            IOpenCast openCast = castFactory.getCast(fromOpenClass, to);
            if (openCast != null && !(openCast instanceof JavaDownCast)) {
                return openCast.convert(from);
            }
            return to.getInstanceClass().cast(from);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenCast#getDistance(org.openl.types.IOpenClass, org.openl.types.IOpenClass)
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
