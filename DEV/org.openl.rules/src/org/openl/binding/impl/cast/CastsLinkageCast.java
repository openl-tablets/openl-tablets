package org.openl.binding.impl.cast;

import java.util.ArrayList;
import java.util.Objects;

public final class CastsLinkageCast implements IOpenCast {

    private final IOpenCast[] casts;
    private int distance;

    public CastsLinkageCast(IOpenCast... casts) {
        this.casts = optimizeCasts(Objects.requireNonNull(casts, "casts cannot be null"));
        for (IOpenCast cast : this.casts) {
            var d = cast.getDistance();
            if (distance < d) {
                distance = d;
            }
        }
    }

    private IOpenCast[] optimizeCasts(IOpenCast[] casts) {
        var openCasts = new ArrayList<IOpenCast>();
        for (IOpenCast cast : casts) {
            if (cast instanceof JavaUpCast && !openCasts.isEmpty() && openCasts
                    .getLast() instanceof JavaBoxingCast) {
                openCasts.set(openCasts.size() - 1, JavaBoxingUpCast.getInstance());
            } else {
                openCasts.add(cast);
            }
        }
        return openCasts.toArray(new IOpenCast[0]);
    }

    @Override
    public Object convert(Object from) {
        if (from == null) {
            return null;
        }
        var ret = from;
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
