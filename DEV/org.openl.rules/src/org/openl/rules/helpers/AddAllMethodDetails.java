package org.openl.rules.helpers;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodDetails;
import org.openl.types.IOpenClass;

/**
 * Implementation of {@link MethodDetails} for addAll method from {@link RulesUtils}.
 */
public class AddAllMethodDetails implements MethodDetails {
    private final Integer minDim;
    private final Integer maxDim;
    private final IOpenClass type;
    private final boolean[] paramsAsElement;
    private final IOpenCast[] openCasts;

    public AddAllMethodDetails(Integer minDim,
            Integer maxDim,
            IOpenClass type,
            boolean[] paramsAsElement,
            IOpenCast[] openCasts) {
        this.minDim = minDim;
        this.maxDim = maxDim;
        this.type = type;
        this.paramsAsElement = paramsAsElement;
        this.openCasts = openCasts;
    }

    public Integer getMinDim() {
        return minDim;
    }

    public Integer getMaxDim() {
        return maxDim;
    }

    public IOpenClass getType() {
        return type;
    }

    public boolean[] getParamsAsElement() {
        return paramsAsElement;
    }

    public IOpenCast[] getOpenCasts() {
        return openCasts;
    }
}
