package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.openl.util.StringUtils;

/**
 * Groovy migration: replaces the math functions and constants removed from {@code RulesUtils} in OpenL Tablets 7.0.0
 * with the members of {@code java.lang.Math}, which the removed ones called, as the migration notes of 7.0.0
 * describe. {@code RulesUtils.sqrt(x)} becomes {@code Math.sqrt(x)}, and so does a static import of the function.
 * Texts and comments stay as they are.
 * <p>
 * Two functions have another name in {@code Math}: {@code getExponent(x, y)} with two arguments is
 * {@code Math.hypot(x, y)}, and {@code nextAfter(x)} with one argument is {@code Math.nextUp(x)}. Their static imports
 * stay for a manual fix, as the import does not tell the number of arguments. So does a static import of all the
 * members of {@code RulesUtils}, which also brings the functions that stay there.
 * <p>
 * Migrator id: {@code groovy.removed-math}.
 *
 * @author Yury Molchan
 */
public final class GroovyRemovedMathMigrator extends AbstractMigrator {

    private static final Set<String> MEMBERS = Set.of("abs", "acos", "asin", "atan", "atan2", "cbrt", "ceil",
            "copySign", "cos", "cosh", "exp", "expm1", "floor", "getExponent", "IEEEremainder", "log", "log10", "log1p",
            "nextAfter", "pow", "random", "rint", "scalb", "signum", "sin", "sinh", "sqrt", "tan", "tanh", "toDegrees",
            "toRadians", "ulp", "E", "PI");
    /**
     * The functions that have another name in {@code Math} when called with the number of arguments.
     */
    private static final Map<String, Map<Integer, String>> RENAMED = Map.of("getExponent", Map.of(2, "hypot"),
            "nextAfter", Map.of(1, "nextUp"));
    private static final String RULES_UTILS = "org.openl.rules.helpers.RulesUtils";
    private static final Pattern MEMBER = Pattern.compile(
            "(?<!\\.)\\b(?:" + Pattern.quote(RULES_UTILS) + "|RulesUtils)\\s*\\.\\s*(\\w+)\\b");
    private static final Pattern STATIC_IMPORT = Pattern.compile(
            "\\bimport\\s+static\\s+" + Pattern.quote(RULES_UTILS) + "\\s*\\.\\s*(\\w+)\\b");

    public GroovyRemovedMathMigrator() {
        super("groovy.removed-math",
                "groovy math from RulesUtils to java.lang.Math",
                """
                Replaces the math functions and the PI and E constants removed from RulesUtils with those of
                java.lang.Math in every .groovy file under the project: RulesUtils.sqrt(x) becomes
                Math.sqrt(x). getExponent(x, y) becomes Math.hypot(x, y) and nextAfter(x) becomes
                Math.nextUp(x); their static imports, and a static import of all of RulesUtils, stay for a
                manual fix.
                """);
    }

    @Override
    public List<Path> migrate(Path sourceFolder, MigrationContext context) throws IOException {
        return GroovySourcesIO.rewrite(sourceFolder, GroovyRemovedMathMigrator::migrate);
    }

    /**
     * Replaces the removed math members of {@code RulesUtils} the source uses.
     */
    static String migrate(String source) {
        var imported = STATIC_IMPORT.matcher(source).replaceAll(GroovyRemovedMathMigrator::staticImport);
        var literals = GroovyText.literals(imported);
        return MEMBER.matcher(imported)
                .replaceAll(member -> literals.get(member.start()) ? member.group() : member(member, imported));
    }

    private static String staticImport(MatchResult member) {
        var name = member.group(1);
        return MEMBERS.contains(name) && !RENAMED.containsKey(name)
                ? "import static java.lang.Math." + name
                : member.group();
    }

    /**
     * The member of {@code Math} for the member of {@code RulesUtils}. A function that has another name for some
     * numbers of arguments stays where it is not called.
     */
    private static String member(MatchResult member, String source) {
        var name = member.group(1);
        if (!MEMBERS.contains(name)) {
            return member.group();
        }
        var renamed = RENAMED.get(name);
        if (renamed == null) {
            return "Math." + name;
        }
        var arguments = arguments(source, member.end());
        return arguments < 0 ? member.group() : "Math." + renamed.getOrDefault(arguments, name);
    }

    /**
     * Returns the number of arguments of the call whose parentheses start after the position, or {@code -1} when no
     * call starts there.
     */
    private static int arguments(String source, int position) {
        var open = StringUtils.firstNonSpace(source, position, source.length());
        var arguments = open < 0 || source.charAt(open) != '(' ? null : CodeText.arguments(source, open);
        return arguments == null ? -1 : arguments.size();
    }
}
