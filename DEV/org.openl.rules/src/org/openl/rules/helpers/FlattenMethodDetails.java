package org.openl.rules.helpers;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodDetails;
import org.openl.types.IOpenClass;

/**
 * Implementation of {@link MethodDetails} for flatten method from {@link RulesUtils}.
 */
public class FlattenMethodDetails implements MethodDetails {
    private final IOpenClass type;
    private final int[] dims;
    private final IOpenCast[] openCasts;

    public FlattenMethodDetails(IOpenClass type, int[] dims, IOpenCast[] openCasts) {
        this.type = type;
        this.openCasts = openCasts;
        this.dims = dims;
    }

    public IOpenClass getType() {
        return type;
    }

    public IOpenCast[] getOpenCasts() {
        return openCasts;
    }

    public int[] getDims() {
        return dims;
    }
}
