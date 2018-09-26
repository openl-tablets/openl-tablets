package org.openl.binding.impl.cast;

import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;

public class AliasToTypeCast implements IOpenCast {

    private IOpenClass fromClass;
    // private IOpenClass toClass;

    public AliasToTypeCast(IOpenClass from, IOpenClass to) {
        this.fromClass = from;
        // this.toClass = to;
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }

        @SuppressWarnings("rawtypes")
        IDomain domain = fromClass.getDomain();

        // Try to get given object from type domain. If object belongs to domain
        // true value
        // ill be returned; false - otherwise.
        // NOTE: EnumDomain implementation of IDomain (used by alias types)
        // throws runtime exception if object doesn't belong to domain.
        //
        boolean isInDomain = domain.selectObject(from);

        // If object doesn't belong to domain throw runtime exception with
        // appropriate message.
        //
        if (!isInDomain) {
            throw new RuntimeException("Object '" + from + "' is outside of a valid domain");
        }

        // Return object as a converted value.
        //
        return from;
    }

    public int getDistance() {
        return CastFactory.ALIAS_TO_TYPE_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }

}
