package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

/**
 * Presents absent casting. This class is used to replace null value in cache.
 */
public final class CastNotFound implements IOpenCast {

    private CastNotFound() {
        // Use CastNotFound.instance.
    }

    /**
     * Impossible to convert.
     */
    public Object convert(Object from) {
        throw new ClassCastException("Unsupported conversion");
    }

    /**
     * Maximal possible distance.
     */
    public int getDistance(IOpenClass from, IOpenClass to) {
        return Integer.MAX_VALUE;
    }

    /**
     * Cannot be performed automaticaly.
     */
    public boolean isImplicit() {
        return false;
    }

    public static IOpenCast instance = new CastNotFound();
}
