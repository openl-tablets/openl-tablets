package org.openl.rules.dt.element;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;

public final class ConditionHelper {
    private ConditionHelper() {
    }

    private static IOpenCast toNullIfNotImplicitCastAndNotOneElementArrayCast(IOpenCast cast) {
        if (cast != null && cast.isImplicit() && !(cast instanceof IOneElementArrayCast)) {
            return cast;
        }
        return null;
    }

    public static ConditionCasts findConditionCasts(IOpenClass conditionParameterType,
            IOpenClass inputType,
            IBindingContext bindingContext) {
        IOpenCast castToConditionType = toNullIfNotImplicitCastAndNotOneElementArrayCast(
            bindingContext.getCast(inputType, conditionParameterType));
        IOpenCast castToInputType = castToConditionType == null ? toNullIfNotImplicitCastAndNotOneElementArrayCast(
            bindingContext.getCast(conditionParameterType, inputType)) : null;
        return new ConditionCasts(castToInputType, castToConditionType);
    }

    public static ConditionCasts getConditionCastsWithNoCasts() {
        return CONDITION_CASTS_WITH_NO_CASTS;
    }

    private static final ConditionCasts CONDITION_CASTS_WITH_NO_CASTS = new ConditionCasts(null, null);
}
