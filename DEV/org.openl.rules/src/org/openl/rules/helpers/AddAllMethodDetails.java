package org.openl.rules.helpers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodDetails;
import org.openl.types.IOpenClass;

/**
 * Implementation of {@link MethodDetails} for addAll method from {@link RulesUtils}.
 */
@RequiredArgsConstructor
public class AddAllMethodDetails implements MethodDetails {
    @Getter
    private final Integer minDim;
    @Getter
    private final Integer maxDim;
    @Getter
    private final IOpenClass type;
    @Getter
    private final boolean[] paramsAsElement;
    @Getter
    private final IOpenCast[] openCasts;
}
