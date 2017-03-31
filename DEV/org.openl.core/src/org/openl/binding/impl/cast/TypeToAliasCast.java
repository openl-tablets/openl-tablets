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
     * Original type of object before type conversion.
     */
//	private IOpenClass fromClass;

    /**
     * Result type of object after conversion.
     */
    private IOpenClass toClass;

    public TypeToAliasCast(IOpenClass from, IOpenClass to) {
//        this.fromClass = from;
        this.toClass = to;
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }

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
                String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s", from, toClass.getName(), DomainUtils.toString(domain)));
        }

        // Return object as a converted value.
        //
        return from;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return 4;
    }

    public boolean isImplicit() {
        return true;
    }

}
