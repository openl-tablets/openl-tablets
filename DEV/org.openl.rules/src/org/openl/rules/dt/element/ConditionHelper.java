package org.openl.rules.dt.element;

import org.jspecify.annotations.NonNull;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.OpenClassUtils;

public final class ConditionHelper {
    private ConditionHelper() {
    }

    private static IOpenCast toNullIfNotImplicitCastAndNotOneElementArrayCast(IOpenCast cast) {
        if (cast != null && cast.isImplicit() && !(cast instanceof IOneElementArrayCast)) {
            return cast;
        }
        return null;
    }

    /**
     * Finds how the value a condition is asked about and the values of its column are brought to one type.
     *
     * <p>The values are compared in the type {@link #comparedType} names. For a column of an alias type, or of arrays of
     * one, asked about a value of another type, that is the Java type the alias stands for, so the domain of the alias
     * is never checked for the value asked about.
     */
    public static ConditionCasts findConditionCasts(IOpenClass conditionParameterType,
                                                    IOpenClass inputType,
                                                    IBindingContext bindingContext) {
        var comparedType = comparedType(conditionParameterType, inputType);
        IOpenCast castToConditionType = toNullIfNotImplicitCastAndNotOneElementArrayCast(
                bindingContext.getCast(inputType, comparedType));
        IOpenCast castToInputType = castToConditionType == null ? toNullIfNotImplicitCastAndNotOneElementArrayCast(
                bindingContext.getCast(comparedType, inputType)) : null;
        return new ConditionCasts(castToInputType, castToConditionType);
    }

    /**
     * Returns the type a condition compares the value it is asked about with the values of its column in.
     *
     * <p>It is the type of the column, except for a column of an alias type, or of arrays of one, asked about a value
     * of another type. Such values are compared in the Java type the alias stands for, as the expression of the
     * condition compares them. A value outside the domain of the alias then equals no value of the column rather than
     * failing the call, and the answer is the same whether the condition is indexed or evaluated rule by rule.
     *
     * @param conditionParameterType type of the condition column
     * @param inputType              type of the value the condition is asked about
     * @return the type the two are compared in
     */
    static @NonNull IOpenClass comparedType(@NonNull IOpenClass conditionParameterType,
                                            @NonNull IOpenClass inputType) {
        if (OpenClassUtils.getRootComponentClass(conditionParameterType) instanceof DomainOpenClass
                && !(OpenClassUtils.getRootComponentClass(inputType) instanceof DomainOpenClass)) {
            // An array of an alias stands for an array of the Java type of the alias.
            return JavaOpenClass.getOpenClass(conditionParameterType.getInstanceClass());
        }
        return conditionParameterType;
    }

    public static ConditionCasts getConditionCastsWithNoCasts() {
        return CONDITION_CASTS_WITH_NO_CASTS;
    }

    private static final ConditionCasts CONDITION_CASTS_WITH_NO_CASTS = new ConditionCasts(null, null);
}
