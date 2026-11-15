package org.openl.rules.dt.algorithm;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.binding.ICastFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.element.ConditionHelper;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.helpers.StringRange;
import org.openl.types.IOpenClass;
import org.openl.types.Invokable;
import org.openl.types.NullParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.OpenClassUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Gives the value a condition is asked about in the type of the values of its column.
 *
 * <p>It serves a condition whose expression has a wider type than its column, such as {@code map.get("key")} or
 * {@code list[0]} of type {@code Object} asked about a column of {@code Boolean} values. The type of such a value is
 * known only when the rules run. A value of the column type is kept as it is. A value of another type is converted by
 * the implicit cast OpenL applies to an argument of a call, so an {@code Integer} matches a column of {@code Double}
 * values.
 *
 * <p>A value that has no implicit cast to the column type, such as the text {@code "true"} for a column of
 * {@code Boolean} values, fails the call: it could never equal a value of the column. An empty value is kept and matches
 * as before.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
final class TypedConditionValue implements Invokable {

    private final @NonNull Invokable expression;
    private final @NonNull IOpenClass type;
    private final @NonNull String conditionName;
    private final @NonNull String tableName;
    private final @NonNull ICastFactory castFactory;

    /**
     * Returns the type a condition with one parameter brings the value it is asked about to.
     *
     * <p>That is the type of the values of the column, or of the elements of a column of arrays, when the expression of
     * the condition has a wider type. A column of ranges, of a wider type or of the type of the expression needs no
     * conversion.
     *
     * @param condition      condition to check
     * @param expressionType type of the expression of the condition
     * @return the type of the values of the column, or {@code null} when the value needs no conversion
     */
    static @Nullable IOpenClass findType(@NonNull ICondition condition, @NonNull IOpenClass expressionType) {
        var params = condition.getParams();
        var expressionClass = expressionType.getInstanceClass();
        if (params.length != 1 || NullParameterDeclaration.isAnyNull(params) || expressionClass == null
                || expressionType.isArray()) {
            return null;
        }
        var columnType = params[0].getType();
        if (columnType.isArray()) {
            columnType = columnType.getComponentClass();
        }
        var type = OpenClassUtils.toWrapperIfPrimitive(ConditionHelper.comparedType(columnType, expressionType));
        var typeClass = type.getInstanceClass();
        if (typeClass == null || typeClass == expressionClass || !expressionClass.isAssignableFrom(typeClass)
                || isRange(typeClass)) {
            return null;
        }
        return type;
    }

    private static boolean isRange(@NonNull Class<?> type) {
        return INumberRange.class.isAssignableFrom(type) || DateRange.class.isAssignableFrom(type)
                || CharRange.class.isAssignableFrom(type) || StringRange.class.isAssignableFrom(type);
    }

    @Override
    public @Nullable Object invoke(@Nullable Object target,
                                   @Nullable Object @NonNull [] params,
                                   @NonNull IRuntimeEnv env) {
        Object value = expression.invoke(target, params, env);
        if (value == null || type.getInstanceClass().isInstance(value)) {
            return value;
        }
        var cast = castFactory.getCast(JavaOpenClass.getOpenClass(value.getClass()), type);
        if (cast == null || !cast.isImplicit()) {
            throw new OpenLRuntimeException(
                    "Condition '%s' of table '%s' expects a value of type '%s', but got '%s' of type '%s'.".formatted(
                            conditionName,
                            tableName,
                            type.getDisplayName(INamedThing.SHORT),
                            ArrayUtils.toString(value),
                            value.getClass().getSimpleName()));
        }
        return cast.convert(value);
    }
}
