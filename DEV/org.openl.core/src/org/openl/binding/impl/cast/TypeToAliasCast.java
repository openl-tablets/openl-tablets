package org.openl.binding.impl.cast;

import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;
import org.openl.util.DomainUtils;

/**
 * Class provides feature to convert alias data type to underlying type.
 * 
 * Alias data type is special type in OpenL engine. This type used by engine as
 * helpful mechanism to provide for users ability to use types with predefined
 * values. For example, person age is int value, but it cannot be negative. For
 * this reason user can define alias type Age with underlying type IntRange and
 * define appropriate values for age field ( e.g. [0 .. 150]). If user can try
 * to set value which is not contained in range runtime exception will be
 * thrown. For simplicity, alias type is just runtime validation of variable
 * values.
 * 
 * @see IOpenCast
 */
public class TypeToAliasCast implements IOpenCast {

    /**
     * Result type of object after conversion.
     */
    private IOpenClass toClass;
    private int distance = CastFactory.TYPE_TO_ALIAS_CAST_DISTANCE;
    private IOpenCast typeCast;

    public TypeToAliasCast(IOpenClass to) {
        this.toClass = to;
    }

    public TypeToAliasCast(IOpenClass to, IOpenCast typeCast) {
        this.toClass = to;
        this.typeCast = typeCast;
        distance = typeCast.getDistance() - 1;//This cast has higher priority
    }

    public Object convert(Object from) {
        if (typeCast != null) {
            from = typeCast.convert(from);
        }

        if (from == null) {
            return null;
        }

        @SuppressWarnings("rawtypes")
        IDomain domain = toClass.getDomain();

        // Try to get given object from type domain. If object belongs to domain
        // true value
        // ill be returned; false - otherwise.
        // NOTE: EnumDomain implementation of IDomain (used by alias types)
        // throws runtime exception if object doesn't belong to domain.
        //
        @SuppressWarnings("unchecked")
        boolean isInDomain = domain.selectObject(from);

        // If object doesn't belong to domain throw runtime exception with
        // appropriate message.
        //
        if (!isInDomain) {
            throw new OutsideOfValidDomainException(
                String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                    from,
                    toClass.getName(),
                    DomainUtils.toString(domain)));
        }

        // Return object as a converted value.
        //
        return from;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isImplicit() {
        return true;
    }

}
