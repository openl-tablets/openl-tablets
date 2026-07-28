package org.openl.studio.projects.service.trace;
import java.lang.reflect.Array;
import java.util.Date;

import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.studio.projects.model.trace.FrameKind;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.util.OpenClassUtils;

/**
 * Builds the classic trace's detailed titles, which carry the computed values.
 *
 * <p>A table node reads as its kind, signature, and result — for example
 * {@code DT Double NonZeroValues(Double value) = 754299}. A spreadsheet cell reads as its value.
 *
 * <p>These titles hold the run's values, so they are built only for the business view and only on request;
 * the advanced debugger keeps the tree lean, without them.
 */
final class TraceTitleFormatter {

    private TraceTitleFormatter() {
    }

    /**
     * A table node's title: its kind prefix, method signature, and result. A failed table reads
     * {@code = ERROR}; a void method has no {@code =} suffix at all.
     */
    static String tableTitle(FrameKind kind, ExecutableRulesMethod method, @Nullable Object result, boolean error) {
        StringBuilder buf = new StringBuilder(64);
        String prefix = prefix(kind);
        if (prefix != null) {
            buf.append(prefix).append(' ');
        }
        MethodUtil.printMethod(method, buf);
        if (error) {
            buf.append(" = ERROR");
        } else {
            String returned = formatValue(method.getType(), result);
            if (returned != null) {
                buf.append(" = ").append(returned);
            }
        }
        return buf.toString();
    }

    /** The value shown after a spreadsheet cell's name, or {@code null} when there is nothing to show. */
    static @Nullable String cellValue(IOpenClass type, @Nullable Object value) {
        return formatValue(type, value);
    }

    /** The classic per-kind title prefix; {@code null} for a step reference, which is not a table. */
    private static @Nullable String prefix(FrameKind kind) {
        return switch (kind) {
            case DECISION_TABLE -> "DT";
            case SPREADSHEET -> "SpreadSheet";
            case METHOD -> "Method table";
            case COLUMN_MATCH -> "CM";
            case TBASIC -> "Algorithm";
            case TBASIC_METHOD -> "Algorithm Method";
            case STEP_REF -> null;
        };
    }

    /**
     * The value shown after {@code =}, as the classic trace showed it. Only a formattable type reads as its
     * value: a simple value (number, string, date, boolean) or a collection whose elements are simple. A
     * complex object reads as its type name; a non-empty complex collection as {@code Collection of <element>};
     * an empty collection as {@code {}}. A void method has no value at all ({@code null} is returned).
     */
    private static @Nullable String formatValue(IOpenClass type, @Nullable Object value) {
        if (OpenClassUtils.isVoid(type)) {
            return null;
        }
        if (value == null) {
            return "null";
        }
        if (canBeFormatted(type)) {
            return format(value);
        }
        if (isCollection(type)) {
            return isEmpty(type, value) ? "{}" : "Collection of " + componentName(type);
        }
        return type.getDisplayName(INamedThing.SHORT);
    }

    /** A type reads as a value only when it is simple, a date, or a collection whose elements are, recursively. */
    private static boolean canBeFormatted(IOpenClass type) {
        if (type.isSimple() || isDate(type)) {
            return true;
        }
        if (isCollection(type)) {
            IOpenClass component = type.getComponentClass();
            return component != null && canBeFormatted(component);
        }
        return false;
    }

    private static boolean isCollection(IOpenClass type) {
        IAggregateInfo aggregate = type.getAggregateInfo();
        return aggregate != null && aggregate.isAggregate(type);
    }

    private static boolean isDate(IOpenClass type) {
        Class<?> instance = type.getInstanceClass();
        return instance != null && Date.class.isAssignableFrom(instance);
    }

    private static boolean isEmpty(IOpenClass type, Object value) {
        return !type.getAggregateInfo().getIterator(value).hasNext();
    }

    private static String componentName(IOpenClass type) {
        IOpenClass component = type.getComponentClass();
        return (component == null ? type : component).getDisplayName(INamedThing.SHORT);
    }

    private static String format(Object value) {
        if (value instanceof Number) {
            return FormattersManager.getFormatter(value.getClass(), null).format(value);
        }
        if (value.getClass().isArray()) {
            StringBuilder buf = new StringBuilder("{");
            for (int i = 0; i < Array.getLength(value); i++) {
                if (i > 0) {
                    buf.append(',');
                }
                Object element = Array.get(value, i);
                buf.append(element == null ? "null" : format(element));
            }
            return buf.append('}').toString();
        }
        try {
            return FormattersManager.format(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
