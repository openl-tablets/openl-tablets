package org.openl.binding.impl;

import java.util.Collection;

import org.openl.OpenL;
import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.ThrowableVoidCast.ThrowableVoid;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Contains information needed to cast two types to wider type
 */
public final class CastToWiderType {
    private final IOpenClass widerType;
    private final IOpenCast cast1;
    private final IOpenCast cast2;

    private CastToWiderType(IOpenClass widerType, IOpenCast cast1, IOpenCast cast2) {
        this.widerType = widerType;
        this.cast1 = cast1;
        this.cast2 = cast2;
    }

    public IOpenClass getWiderType() {
        return widerType;
    }

    /**
     * The cast from type1 to {@link #widerType}
     *
     * @return Type cast. Can be null if cast is not needed
     */
    public IOpenCast getCast1() {
        return cast1;
    }

    /**
     * The cast from type2 to {@link #widerType}
     *
     * @return Type cast. Can be null if cast is not needed
     */
    public IOpenCast getCast2() {
        return cast2;
    }

    /**
     * Find wider type for types type1 and type 2 and needed casts for them.
     *
     * @param castFactory binding context
     * @param type1 first type
     * @param type2 second type
     * @return cast information
     */
    public static CastToWiderType create(ICastFactory castFactory, IOpenClass type1, IOpenClass type2) {
        if (NullOpenClass.the.equals(type1) && NullOpenClass.the.equals(type2)) {
            return new CastToWiderType(type2, null, null);
        } else if (ThrowableVoid.class.equals(type1.getInstanceClass())) {
            IOpenCast cast = castFactory.getCast(type1, type2);
            return new CastToWiderType(type2, cast, null);
        } else if (ThrowableVoid.class.equals(type2.getInstanceClass())) {
            IOpenCast cast = castFactory.getCast(type2, type1);
            return new CastToWiderType(type1, null, cast);
        } else {
            IOpenCast cast1To2 = castFactory.getCast(type1, type2);
            IOpenCast cast2To1 = castFactory.getCast(type2, type1);

            if (cast1To2 == null || cast2To1 == null) {
                // Find parent class for cast both nodes
                IOpenClass parentClass = castFactory.findClosestClass(type1, type2);
                if (parentClass != null) {
                    IOpenCast castToParent1 = castFactory.getCast(type1, parentClass);
                    IOpenCast castToParent2 = castFactory.getCast(type2, parentClass);
                    return new CastToWiderType(parentClass, castToParent1, castToParent2);
                }
            } else {
                if (!cast1To2.isImplicit() && cast2To1.isImplicit()) {
                    return new CastToWiderType(type1, null, cast2To1);
                } else {
                    if (!cast2To1.isImplicit() && cast1To2.isImplicit()) {
                        return new CastToWiderType(type2, cast1To2, null);
                    } else {
                        if (cast1To2.isImplicit() && cast2To1.isImplicit()) {
                            if (cast1To2.getDistance() < cast2To1.getDistance()) {
                                return new CastToWiderType(type2, cast1To2, null);
                            } else {
                                return new CastToWiderType(type1, null, cast2To1);
                            }
                        }
                    }
                }
            }
            if (type1 instanceof DomainOpenClass || type2 instanceof DomainOpenClass) {
                return create(castFactory,
                    JavaOpenClass.getOpenClass(type1.getInstanceClass()),
                    JavaOpenClass.getOpenClass(type2.getInstanceClass()));
            }
            return new CastToWiderType(type1, null, null);
        }

    }

    // TODO remove after adding a support of generics to OpenL
    public static IOpenClass defineCollectionWiderType(Collection<?> collection) {
        if (collection == null) {
            return NullOpenClass.the;
        }
        ICastFactory castFactory = OpenL.getInstance(OpenL.OPENL_JAVA_NAME).getBinder().getCastFactory();
        IOpenClass widerType = null;
        for (Object ob : collection) {
            if (ob != null) {
                if (widerType == null) {
                    widerType = JavaOpenClass.getOpenClass(ob.getClass());
                    continue;
                }
                IOpenClass fieldType = JavaOpenClass.getOpenClass(ob.getClass());
                widerType = create(castFactory, widerType, fieldType).getWiderType();
            }
        }
        if (widerType == null) {
            return NullOpenClass.the;
        }
        return widerType;
    }
}
