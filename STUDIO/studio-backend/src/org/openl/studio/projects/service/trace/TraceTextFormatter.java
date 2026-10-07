package org.openl.studio.projects.service.trace;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.studio.projects.model.trace.FrameKind;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.OpenClassUtils;
import org.openl.util.formatters.IFormatter;

/**
 * Renders trace nodes as the plain-text lines of a {@code trace.txt} export.
 *
 * <p>Reproduces the legacy trace format: a table frame reads {@code <kind> <signature> = <result>}, a
 * spreadsheet cell reads {@code <label> = <value>}, and a decision-table check reads
 * {@code Condition: <name>, Rules: [<rules>]}. Numbers keep full precision when real numbers are shown.
 */
@Slf4j
final class TraceTextFormatter {

    private TraceTextFormatter() {
    }

    /** Short prefix naming the table kind, as the legacy trace printed it (for example {@code SpreadSheet}). */
    static String prefix(FrameKind kind) {
        return switch (kind) {
            case SPREADSHEET -> "SpreadSheet";
            case DECISION_TABLE -> "DT";
            case METHOD -> "Method";
            case COLUMN_MATCH -> "ColumnMatch";
            case TBASIC, TBASIC_METHOD -> "TBasic";
            case STEP_REF -> "Ref";
        };
    }

    /** A frame line: {@code SpreadSheet Double Rate(int age) = 0.9}. */
    static String frameLine(ExecutableRulesMethod method, FrameKind kind, Object result, boolean smartNumbers) {
        String header = frameHeader(method, kind);
        String value = valueOfType(method.getType(), result, smartNumbers);
        return value != null ? header + " = " + value : header;
    }

    /** A frame line for a table that threw: {@code DT Double Rate(int age) = ERROR}. */
    static String frameErrorLine(ExecutableRulesMethod method, FrameKind kind) {
        return frameHeader(method, kind) + " = ERROR";
    }

    private static String frameHeader(ExecutableRulesMethod method, FrameKind kind) {
        var buf = new StringBuilder(64);
        buf.append(prefix(kind)).append(' ');
        MethodUtil.printMethod(method, buf);
        return buf.toString();
    }

    /** A spreadsheet cell line: {@code $Value$Rate = 0.9}. */
    static String cellLine(String label, IOpenClass type, Object value, boolean smartNumbers) {
        String rendered = valueOfType(type, value, smartNumbers);
        return rendered == null ? label : label + " = " + rendered;
    }

    /** A decision-table condition line: {@code Condition: MC1, Rules: [R3]}. */
    static String conditionLine(String conditionName, List<String> ruleNames) {
        return "Condition: " + conditionName + ", Rules: " + ruleNames;
    }

    /** A fired-rule line: {@code Returned rule: [R3]}. */
    static String returnedRuleLine(List<String> ruleNames) {
        return "Returned rule: " + ruleNames;
    }

    /** Render a value against its declared type, mirroring the legacy display: {@code null} means "no value". */
    private static String valueOfType(IOpenClass type, Object value, boolean smartNumbers) {
        if (type != null && OpenClassUtils.isVoid(type)) {
            return null;
        }
        if (value == null) {
            return "null";
        }
        if (canBeFormatted(type)) {
            return format(value, smartNumbers);
        }
        // Reached only when type is a non-simple, non-collection class, so it is never null here.
        return type.getDisplayName(INamedThing.SHORT);
    }

    private static boolean canBeFormatted(IOpenClass type) {
        return type == null || type.isSimple() || ClassUtils.isAssignable(type.getInstanceClass(), Date.class);
    }

    /**
     * Render a non-void, non-null value: a number (with full precision when {@code smartNumbers}), an array
     * as {@code {a,b}}, else the type's formatter with a {@code String.valueOf} fallback. Shared with
     * {@link TraceTitleFormatter}, which layers its collection handling on top.
     */
    static String format(Object o, boolean smartNumbers) {
        if (o == null) {
            // A null element inside an array; the top-level null is already handled by the caller.
            return "null";
        }
        if (o instanceof Number) {
            IFormatter formatter = FormattersManager.getFormatter(o.getClass(),
                    smartNumbers ? null : FormattersManager.DEFAULT_NUMBER_FORMAT);
            return formatter.format(o);
        }
        if (o.getClass().isArray()) {
            var sb = new StringBuilder("{");
            for (var i = 0; i < Array.getLength(o); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(format(Array.get(o, i), smartNumbers));
            }
            return sb.append('}').toString();
        }
        try {
            return FormattersManager.format(o);
        } catch (RuntimeException e) {
            log.debug("Failed to format traced value of type {}", o.getClass().getName(), e);
            return String.valueOf(o);
        }
    }
}
