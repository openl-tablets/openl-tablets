package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 * Rules migration: rewrites the calls of the functions removed in OpenL Tablets 7.0.0 — {@code format},
 * {@code dateToString}, {@code stringToDate}, {@code parseFormattedDouble}, {@code addIgnoreNull} and
 * {@code addArrayElementIgnoreNull} — and the {@code DEFAULT_DOUBLE_FORMAT} constant, as the migration notes of
 * 7.0.0 describe.
 * <p>
 * A call is rewritten by the types of its arguments, so {@code format(amount)} and {@code format(date)} get
 * different patterns, while {@code format("{0} days", n)}, which calls the {@code format} function that stays,
 * compiles and is left as it is. {@code parseFormattedDouble(text, pattern)} has no direct replacement and stays, as
 * does an {@code addIgnoreNull} whose array or element is computed by a call, which the replacement would run
 * twice.
 * <p>
 * The replacements use US English instead of the language of the server, and return an empty value where the removed
 * functions stopped the call with an error. A removed function, a constant or an argument type is added by one more
 * entry of {@link #FIXES}.
 * <p>
 * Migrator id: {@code rules.removed-functions}.
 *
 * @author Yury Molchan
 */
public final class RulesRemovedFunctionsMigrator implements Migrator {

    private static final String FORMAT = "format";
    private static final String DATE = "toString({0}, \"M/d/yy\")";
    private static final String NUMBER = "toString({0}, \"#,##0.00\")";
    private static final String WITH_PATTERN = "toString({0}, {1})";
    private static final String ADD = "{1} == null ? {0} : add({0}, {1})";
    private static final String ADD_AT = "{2} == null ? {0} : addElement({0}, {1}, {2})";

    private static final List<CodeFix> FIXES = List.of(
            CodeFix.function(FORMAT, DATE, Date.class),
            CodeFix.function(FORMAT, WITH_PATTERN, Date.class, String.class),
            CodeFix.function(FORMAT, NUMBER, Number.class),
            CodeFix.function(FORMAT, WITH_PATTERN, Number.class, String.class),
            CodeFix.function("dateToString", DATE, Date.class),
            CodeFix.function("dateToString", WITH_PATTERN, Date.class, String.class),
            CodeFix.function("stringToDate", "toDate({0})", String.class),
            CodeFix.function("parseFormattedDouble", "toNumber({0})", String.class),
            CodeFix.function("addIgnoreNull", ADD, Object[].class, Object.class),
            CodeFix.function("addIgnoreNull", ADD_AT, Object[].class, Integer.class, Object.class),
            CodeFix.function("addArrayElementIgnoreNull", ADD, Object[].class, Object.class),
            CodeFix.function("addArrayElementIgnoreNull", ADD_AT, Object[].class, Integer.class, Object.class),
            CodeFix.identifier("DEFAULT_DOUBLE_FORMAT", "\"#,##0.00\""));

    @Override
    public String getId() {
        return "rules.removed-functions";
    }

    @Override
    public String getCommitMessage() {
        return "replace the functions removed in OpenL 7.0.0";
    }

    @Override
    public String getDescription() {
        return """
                Compiles the project and rewrites the rules that fail on the removed format, dateToString,
                stringToDate, parseFormattedDouble, addIgnoreNull, addArrayElementIgnoreNull and
                DEFAULT_DOUBLE_FORMAT, choosing the replacement by the argument types. Assumes US English:
                the replacements ignore the language of the server and return an empty value instead of an
                error. parseFormattedDouble(text, pattern) and an addIgnoreNull whose array or element
                is a call stay for a manual fix.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, MigrationContext context) throws IOException {
        return RulesCodeIO.rewrite(this, sourceFolder, context, FIXES);
    }
}
