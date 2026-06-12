package org.openl.rules.maven.migration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * {@code rules.xml} migration: replaces module-level {@code <method-filter>} with project-level
 * {@code <exposed-methods>} derived from the legacy regexps and the actual methods of the OpenL-generated
 * interface — see {@link #transform} for the derivation rules. The supplier is invoked at most once and
 * only when there is a non-empty filter and no {@code <exposed-methods>} block yet — populated
 * {@code <exposed-methods>} is left untouched.
 * <p>
 * Migrator id: {@code config.project.method-filter}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectMethodFilterMigrator implements Migrator {

    /**
     * Clears module-level {@code <method-filter>} elements. If at least one had content and no populated
     * {@code <exposed-methods>} exists, queries the supplier and converts the legacy include regexps into
     * {@code <exposed-methods>} include patterns. Package-private for direct unit testing.
     * <p>
     * The legacy regexps are matched against {@code ReturnType methodName(ArgumentType1, ArgumentTypeN)}
     * signatures with canonical type names; {@code .+} and {@code .*} are treated as equal. A regexp shaped
     * {@code <wildcard> prefix<wildcard>\(<wildcard>\)} becomes the {@code prefix*} glob when the glob
     * selects exactly the methods the regexp matches; a match-everything regexp becomes the bare {@code *}.
     * Any other matching regexp is unfolded into the matched method names. A regexp that matches no method
     * disappears, and a pattern fully covered by another one is dropped ({@code _api_*} eats
     * {@code _api_deduct}).
     * <p>
     * The generated interface is built with the legacy filter still applied, so its methods are exactly
     * the previously exposed set — every one of them stays exposed. Exclude regexps are not translated
     * because a derived glob could re-expose the methods they hid, so a filter with excludes is unfolded
     * into plain method names.
     */
    static void transform(ProjectDescriptor descriptor, Supplier<Class<?>> generatedInterface) {
        var modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            return;
        }

        var legacyIncludes = new LinkedHashSet<String>();
        var anyFilter = false;
        var anyExcludes = false;
        for (var module : modules) {
            var mf = module.getMethodFilter();
            if (mf != null && (CollectionUtils.isNotEmpty(mf.getIncludes())
                    || CollectionUtils.isNotEmpty(mf.getExcludes()))) {
                anyFilter = true;
                if (mf.getIncludes() != null) {
                    mf.getIncludes().stream().filter(StringUtils::isNotBlank).forEach(legacyIncludes::add);
                }
                anyExcludes |= CollectionUtils.isNotEmpty(mf.getExcludes());
                module.setMethodFilter(null);
            }
        }

        if (!anyFilter) {
            return;
        }

        // Existing populated <exposed-methods> wins — the user has already declared what to expose. We just
        // dropped the legacy filters, no further action.
        var existing = descriptor.getExposedMethods();
        if (existing != null && (CollectionUtils.isNotEmpty(existing.getIncludes())
                || CollectionUtils.isNotEmpty(existing.getExcludes()))) {
            return;
        }

        if (generatedInterface == null) {
            return;
        }
        var interfaceClass = generatedInterface.get();
        if (interfaceClass == null) {
            return;
        }

        var methods = Arrays.stream(interfaceClass.getMethods())
                .map(m -> new MethodView(m.getName(), signatureOf(m)))
                .toList();
        if (methods.isEmpty()) {
            return;
        }

        // Exclude regexps cannot be translated: the methods they hid are absent from the generated
        // interface, so a glob derived from an include could silently re-expose them. With excludes the
        // method names are enumerated as before.
        var includes = convertIncludes(anyExcludes ? Set.of() : legacyIncludes, methods);

        var em = existing != null ? existing : new ExposedMethods();
        em.setIncludes(includes);
        descriptor.setExposedMethods(em);
    }

    /**
     * Converts legacy include regexps into {@code <exposed-methods>} include patterns: {@code prefix*}
     * globs where the regexp shape allows it, the matched method names otherwise. Method names not covered
     * by any produced pattern are added explicitly, so every method of the interface stays exposed.
     * Produced patterns are checked with the same glob matching the runtime applies. The result is sorted.
     */
    private static Set<String> convertIncludes(Collection<String> regexps, List<MethodView> methods) {
        var allNames = methods.stream().map(MethodView::name).collect(Collectors.toCollection(TreeSet::new));
        var patterns = new LinkedHashSet<String>();
        for (var regexp : regexps) {
            var parsed = ParsedPattern.parse(regexp);
            Pattern pattern;
            try {
                pattern = Pattern.compile(parsed.regex());
            } catch (PatternSyntaxException e) {
                continue; // the legacy runtime could never apply such a pattern either
            }
            var matched = methods.stream()
                    .filter(m -> pattern.matcher(m.signature()).matches())
                    .map(MethodView::name)
                    .collect(Collectors.toSet());
            if (matched.isEmpty()) {
                continue; // matches nothing in the built project — the pattern just disappears
            }
            var prefix = parsed.namePrefix();
            if (prefix != null && matched.equals(globSelection(prefix + "*", allNames))) {
                patterns.add(prefix + "*");
            } else {
                patterns.addAll(matched);
            }
        }
        // Never lose an exposed method: the interface is generated with the legacy filter applied, so every
        // visible method was exposed before the migration and must stay exposed after it.
        for (var name : allNames) {
            if (patterns.stream().noneMatch(p -> FileUtils.pathMatches(p, name))) {
                patterns.add(name);
            }
        }
        return patterns.stream()
                .filter(p -> patterns.stream().noneMatch(other -> covers(other, p)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Method names the produced pattern selects under the runtime's own glob matching. */
    private static Set<String> globSelection(String pattern, Collection<String> names) {
        return names.stream().filter(n -> FileUtils.pathMatches(pattern, n)).collect(Collectors.toSet());
    }

    /** True when {@code other} is a {@code prefix*} glob selecting everything {@code pattern} selects. */
    private static boolean covers(String other, String pattern) {
        if (other.equals(pattern) || !other.endsWith("*")) {
            return false;
        }
        var prefix = other.substring(0, other.length() - 1);
        var stripped = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
        return stripped.startsWith(prefix);
    }

    /**
     * Rebuilds the signature string the legacy {@code <method-filter>} regexps are matched against:
     * {@code ReturnType methodName(ArgumentType1, ArgumentTypeN)} with canonical type names.
     */
    private static String signatureOf(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ",
                        method.getReturnType().getCanonicalName() + " " + method.getName() + "(", ")"));
    }

    /** A method of the generated interface: its name and the signature the legacy regexps are matched against. */
    private record MethodView(String name, String signature) {
    }

    /**
     * A legacy regexp prepared for migration. {@code regex} is the pattern with every {@code .+} replaced
     * by {@code .*} — equal for migration purposes. {@code namePrefix} is the literal method-name prefix
     * when the pattern is shaped {@code <wildcard> prefix<wildcard>\(<wildcard>\)}, {@code null} otherwise.
     */
    private record ParsedPattern(String regex, String namePrefix) {

        static ParsedPattern parse(String regexp) {
            var regex = new StringBuilder(regexp.length());
            // Non-empty literal segments; null marks a wildcard, never two in a row.
            var tokens = new ArrayList<String>();
            var literal = new StringBuilder();
            var derivable = true;
            for (int i = 0; i < regexp.length(); i++) {
                char c = regexp.charAt(i);
                if (c == '\\' && i + 1 < regexp.length()) {
                    char escaped = regexp.charAt(++i);
                    regex.append('\\').append(escaped);
                    if (Character.isLetterOrDigit(escaped)) {
                        derivable = false; // \d, \w, \Q… — regex constructs, not plain literals
                    } else {
                        literal.append(escaped);
                    }
                } else if (c == '.' && i + 1 < regexp.length()
                        && (regexp.charAt(i + 1) == '*' || regexp.charAt(i + 1) == '+')) {
                    i++;
                    regex.append(".*");
                    if (!literal.isEmpty()) {
                        tokens.add(literal.toString());
                        literal.setLength(0);
                    }
                    if (tokens.isEmpty() || tokens.getLast() != null) {
                        tokens.add(null);
                    }
                } else {
                    regex.append(c);
                    if (".[]{}()^$?+*|".indexOf(c) >= 0) {
                        derivable = false; // any other regex construct — match it, never derive a glob from it
                    } else {
                        literal.append(c);
                    }
                }
            }
            if (!literal.isEmpty()) {
                tokens.add(literal.toString());
            }
            return new ParsedPattern(regex.toString(), derivable ? derivePrefix(tokens) : null);
        }

        /**
         * Returns the method-name prefix when the tokens are exactly
         * {@code [wildcard, " " + prefix, wildcard, "(", wildcard, ")"]} and the prefix consists of
         * identifier characters only; {@code null} otherwise. The prefix may be empty — a sole wildcard
         * (a match-everything regexp) or a bare {@code " "} head yields {@code ""}, which becomes the
         * match-everything {@code *} glob.
         */
        private static String derivePrefix(List<String> tokens) {
            if (tokens.size() == 1 && tokens.getFirst() == null) {
                return ""; // pure wildcard — the regexp matches everything, so does the '*' glob
            }
            if (tokens.size() != 6
                    || tokens.get(0) != null || tokens.get(2) != null || tokens.get(4) != null
                    || !"(".equals(tokens.get(3)) || !")".equals(tokens.get(5))) {
                return null;
            }
            var head = tokens.get(1);
            if (head.charAt(0) != ' ') {
                return null;
            }
            var prefix = head.substring(1);
            return prefix.chars().allMatch(Character::isJavaIdentifierPart) ? prefix : null;
        }
    }

    @Override
    public String getId() {
        return "config.project.method-filter";
    }

    @Override
    public String getCommitMessage() {
        return "method-filter to exposed-methods";
    }

    @Override
    public String getDescription() {
        return """
                Replaces module-level <method-filter> blocks with a single project-level
                <exposed-methods> derived from the legacy regular expressions and the actual methods
                of the built project. A method-name prefix regexp (e.g. '.+ _api_.+\\(.+\\)') becomes
                the glob '_api_*', a match-everything regexp (e.g. '.*') the bare '*'; any other
                matching regexp is unfolded into the matched method names; '.+' and '.*' are equal.
                Unmatchable patterns disappear, covered patterns are dropped, and a filter with
                excludes is unfolded into plain names.
                An already-populated <exposed-methods> is kept untouched. Requires the project to
                build; if it cannot, the filter is dropped and every method becomes exposed.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface)
            throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder,
                descriptor -> transform(descriptor, generatedInterface));
    }
}
