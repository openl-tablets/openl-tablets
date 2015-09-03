package org.openl.binding.impl.cast;

import java.lang.reflect.Array;

import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;

public class AliasToTypeCast implements IOpenCast {

    private IOpenClass fromClass;
//    private IOpenClass toClass;

    public AliasToTypeCast(IOpenClass from, IOpenClass to) {
        this.fromClass = from;
//        this.toClass = to;
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }

        if (fromClass.isArray()) {
            Object[] fromArray = (Object[]) from;
            // create an array of results
            //
            Object results = Array.newInstance(fromArray.getClass().getComponentType(), fromArray.length);

            // populate the results array by converting single value
            //
            for (int i = 0; i < fromArray.length; i++) {
                Array.set(results, i, convertSingle(fromArray[i]));
            }
            return results;
        } else {
            return convertSingle(from);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Object convertSingle(Object from) {
        if (from == null){
            return null;
        }
        
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
            throw new RuntimeException("Object " + from + " is outside of a valid domain");
        }

        // Return object as a converted value.
        //
        return from;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return 3;
    }

    public boolean isImplicit() {
        return true;
    }

}
