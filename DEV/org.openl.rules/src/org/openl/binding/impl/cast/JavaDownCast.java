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
            var arrayLength = Array.getLength(from);
            if (arrayLength > 0) {
                var minDim = Integer.MAX_VALUE;
                for (var i = 0; i < arrayLength; i++) {
                    var p = calcArrayMinDim(Array.get(from, i));
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
            var t = fromOpenClass;
            var d = 0;
            while (t.isArray()) {
                t = t.getComponentClass();
                d++;
            }
            if (d > 0 && t.getInstanceClass() == Object.class) {
                var dim = calcArrayMinDim(from);
                fromOpenClass = JavaOpenClass.OBJECT.getArrayType(dim);
            }
            var openCast = castFactory.getCast(fromOpenClass, to);
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
