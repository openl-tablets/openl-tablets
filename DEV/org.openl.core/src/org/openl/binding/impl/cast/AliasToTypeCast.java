package org.openl.binding.impl.cast;

import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;

public class AliasToTypeCast implements IOpenCast {

    private IOpenClass fromClass;
    private IOpenCast typeCast;
    private int distance = CastFactory.ALIAS_TO_TYPE_CAST_DISTANCE;

    public AliasToTypeCast(IOpenClass from) {
        this.fromClass = from;
    }

    public AliasToTypeCast(IOpenClass from, IOpenCast typeCast) {
        this.fromClass = from;
        this.typeCast = typeCast;
        distance = typeCast.getDistance() - 1;//This cast has higher priority
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
        if (typeCast != null) {
            from = typeCast.convert(from);
        }
        
        return from;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isImplicit() {
        return true;
    }

}
