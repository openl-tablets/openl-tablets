package org.openl.binding.impl.cast;

public final class CastsLinkageCast implements IOpenCast {

    private IOpenCast[] casts;
    private int distance = 0;

    public CastsLinkageCast(IOpenCast... casts) {
        if (casts == null) {
            throw new IllegalArgumentException();
        }
        this.casts = casts;
        for (IOpenCast cast : casts) {
            int d = cast.getDistance();
            if (distance < d) {
                distance = d;
            }
        }
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }
        Object ret = from;
        for (IOpenCast cast : casts) {
            ret = cast.convert(ret);
        }

        return ret;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isImplicit() {
        return false;
    }
}
