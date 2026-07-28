package org.openl.rules.helpers;

import lombok.Getter;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodDetails;
import org.openl.types.IOpenClass;

/**
 * Implementation of {@link MethodDetails} for flatten method from {@link RulesUtils}.
 */
public class FlattenMethodDetails implements MethodDetails {
    @Getter
    private final IOpenClass type;
    @Getter
    private final int[] dims;
    @Getter
    private final IOpenCast[] openCasts;

    public FlattenMethodDetails(IOpenClass type, int[] dims, IOpenCast[] openCasts) {
        this.type = type;
        this.openCasts = openCasts;
        this.dims = dims;
    }
}
