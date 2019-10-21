package org.openl.binding.impl.cast;

import java.util.Objects;

public final class CastsLinkageCast implements IOpenCast {

    private IOpenCast[] casts;
    private int distance = 0;

    public CastsLinkageCast(IOpenCast... casts) {
        this.casts = Objects.requireNonNull(casts, "casts cannot be null");
        for (IOpenCast cast : casts) {
            int d = cast.getDistance();
            if (distance < d) {
                distance = d;
            }
        }
    }

    @Override
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

    @Override
    public int getDistance() {
        return distance;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }
}
