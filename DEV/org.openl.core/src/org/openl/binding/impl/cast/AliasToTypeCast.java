package org.openl.binding.impl.cast;

public class AliasToTypeCast implements IOpenCast {

    private IOpenCast typeCast;
    private int distance = CastFactory.ALIAS_TO_TYPE_CAST_DISTANCE;
    AliasToTypeCast() {
    }

    AliasToTypeCast(IOpenCast typeCast) {
        this.typeCast = typeCast;
        distance = typeCast.getDistance() - 1;//This cast has higher priority
    }

    public Object convert(Object from) {
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
