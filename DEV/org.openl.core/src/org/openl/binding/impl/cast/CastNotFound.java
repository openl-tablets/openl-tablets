package org.openl.binding.impl.cast;

/**
 * Presents absent casting. This class is used to replace null value in cache.
 */
final class CastNotFound implements IOpenCast {

    public static IOpenCast instance = new CastNotFound();

    private CastNotFound() {
        // Use CastNotFound.instance.
    }

    /**
     * Impossible to convert.
     */
    @Override
    public Object convert(Object from) {
        throw new ClassCastException("Unsupported conversion");
    }

    /**
     * Maximal possible distance.
     */
    @Override
    public int getDistance() {
        return Integer.MAX_VALUE;
    }

    /**
     * Cannot be performed automaticaly.
     */
    @Override
    public boolean isImplicit() {
        return false;
    }
}
