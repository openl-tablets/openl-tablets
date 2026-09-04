package org.openl.rules.project.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * The {@code rules.xml} content migrations OpenL Studio applies to a descriptor in memory, so a caller that
 * already holds a {@link ProjectDescriptor} can normalize it without the Maven plugin.
 *
 * <p>These are the same transforms the {@code openl:migrate} goal runs for the classpath, the cw-processor
 * and the default modules; the goal's migrator classes delegate their transform here so there is one
 * implementation. The method-filter migration is the no-compile variant Studio can run without building the
 * project. The empty-tag, deploy and groovy migrations stay in the plugin — Studio does not run them.
 *
 * <p>The empty-tag cleanup is not applied here: it happens for free when the caller re-serializes the
 * descriptor, as the JAXB {@code beforeMarshal} callbacks drop the empty tags.
 */
public final class RulesXmlMigrations {

    private static final Set<String> DEFAULT_CLASSPATH_PATHS = Set.of("groovy/", "groovy", "lib/*.jar");
    private static final String CW_PROCESSOR = "org.openl.rules.project.resolving.CWPropertyFileNameProcessor";
    private static final String XLSX_EXT = ".xlsx";
    /** The engine's default module wildcards, taken from the model so the two cannot drift. */
    private static final Set<String> DEFAULT_WILDCARDS = ProjectDescriptor.defaultModules().stream()
            .map(Module::getRulesRootPath)
            .collect(Collectors.toUnmodifiableSet());
    /** The leading {@code returnType } part of a method signature. */
    private static final Pattern RETURN_TYPE_PREFIX = Pattern.compile("^[^ (]+ ");
    /** The trailing {@code (params)} part of a method signature. */
    private static final Pattern SIGNATURE_PARAMS = Pattern.compile("\\\\\\(.*\\\\\\)$");
    /** Cap on alternation branches; a pattern that would expand past this is kept as a filter, not converted. */
    private static final int MAX_ALTERNATION_BRANCHES = 256;

    private RulesXmlMigrations() {
    }

    /**
     * Applies the {@code rules.xml} content migrations to the descriptor.
     *
     * <p>Runs the classpath, cw-processor and method-filter migrations, then the default-modules migration.
     * Method-filter runs before default-modules so the module-level filters are lifted to a project-level
     * {@code <exposed-methods>} first; the default-modules collapse then folds the now filter-free modules
     * into folder wildcards without losing anything.
     *
     * @param descriptor the descriptor read from {@code rules.xml}; mutated in place
     */
    public static void apply(ProjectDescriptor descriptor) {
        classpath(descriptor);
        cwProcessor(descriptor);
        methodFilter(descriptor);
        defaultModules(descriptor);
    }

    /**
     * Drops the {@code <classpath>} block when every entry is a path the OpenL resolver already adds
     * implicitly ({@code groovy/}, {@code groovy} or {@code lib/*.jar}). Any other entry keeps the whole
     * block. Windows-style {@code \} separators are normalized to {@code /} before matching.
     */
    public static void classpath(ProjectDescriptor descriptor) {
        var classpath = descriptor.getClasspath();
        if (CollectionUtils.isEmpty(classpath)) {
            return;
        }
        var allDefaults = classpath.stream()
                .allMatch(e -> e != null && DEFAULT_CLASSPATH_PATHS.contains(e.replace('\\', '/')));
        if (allDefaults) {
            descriptor.setClasspath(null);
        }
    }

    /**
     * Drops the discontinued {@code CWPropertyFileNameProcessor} reference from
     * {@code <properties-file-name-processor>}. Any other custom processor class is preserved.
     */
    public static void cwProcessor(ProjectDescriptor descriptor) {
        if (CW_PROCESSOR.equals(descriptor.getPropertiesFileNameProcessor())) {
            descriptor.setPropertiesFileNameProcessor(null);
        }
    }

    /**
     * Lifts module-level {@code <method-filter>} blocks to a single project-level {@code <exposed-methods>}.
     * Each include and exclude regexp is converted to one or more exposed-methods globs — an alternation such
     * as {@code .+ calc(Rate|Premium)\(.+\)} unfolds into the matched names {@code calcRate} and
     * {@code calcPremium} — the results merge with any existing {@code <exposed-methods>}, and the module-level
     * filters are removed.
     *
     * <p>Without compiling the project Studio cannot always reduce a regexp to a glob (a character class, a
     * {@code \d}, an optional quantifier). When any pattern of any module filter does not convert, every
     * {@code <method-filter>} is kept in place and no {@code <exposed-methods>} is written, so the migration
     * never drops a restriction and widens the exposed API. An already-declared {@code <exposed-methods>} is
     * preserved and extended, never replaced.
     *
     * <p>This is the no-compile counterpart of the {@code openl:migrate} goal's method-filter migrator: it
     * derives globs from the regexp text alone, without building the project, so Studio can run it in place.
     */
    public static void methodFilter(ProjectDescriptor descriptor) {
        var modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            return;
        }
        var includes = new LinkedHashSet<String>();
        var excludes = new LinkedHashSet<String>();
        var filtered = new ArrayList<Module>();
        for (var module : modules) {
            var filter = module.getMethodFilter();
            if (filter == null) {
                continue;
            }
            if (!collectGlobs(filter.getIncludes(), includes) || !collectGlobs(filter.getExcludes(), excludes)) {
                // A pattern does not reduce to a clean glob. Keep every filter in place rather than dropping a
                // restriction the no-compile path cannot express, which would widen the exposed API.
                return;
            }
            filtered.add(module);
        }
        if (filtered.isEmpty()) {
            return;
        }
        filtered.forEach(module -> module.setMethodFilter(null));
        var existing = descriptor.getExposedMethods();
        if (existing != null) {
            addAll(existing.getIncludes(), includes);
            addAll(existing.getExcludes(), excludes);
        }
        if (includes.isEmpty() && excludes.isEmpty()) {
            return;
        }
        var exposed = new ExposedMethods();
        if (!includes.isEmpty()) {
            exposed.setIncludes(new HashSet<>(includes));
        }
        if (!excludes.isEmpty()) {
            exposed.setExcludes(new HashSet<>(excludes));
        }
        descriptor.setExposedMethods(exposed);
    }

    /**
     * Converts every pattern to its globs, adding them to {@code target}. A blank pattern is skipped. Returns
     * {@code false} as soon as a pattern does not reduce to a clean glob, so the caller can keep the filter.
     */
    private static boolean collectGlobs(Set<String> patterns, Set<String> target) {
        if (patterns == null) {
            return true;
        }
        for (var pattern : patterns) {
            if (StringUtils.isBlank(pattern)) {
                continue;
            }
            var globs = convertRegexToGlobs(pattern);
            if (globs.isEmpty()) {
                return false;
            }
            target.addAll(globs);
        }
        return true;
    }

    private static void addAll(Set<String> source, Set<String> target) {
        if (source != null) {
            target.addAll(source);
        }
    }

    /**
     * Converts a legacy method-filter regexp (matched against a full method signature) to the exposed-methods
     * globs (matched against the method name only). Returns an empty set when the pattern is not a valid
     * regexp or cannot be reduced to clean globs, so the caller keeps the filter instead of dropping it.
     *
     * <p>The regexp is matched against a {@code returnType methodName(argType1, argTypeN)} signature. Common
     * shapes reduce as {@code .+ methodName\(.+\)} to {@code methodName}, {@code .*} or {@code .+} to the bare
     * {@code *}, and {@code .*methodName.*} to {@code *methodName*}. An alternation — a {@code (a|b)} group or
     * a top-level {@code sig1|sig2} — unfolds into one glob per branch, so {@code .+ calc(Rate|Premium)\(.+\)}
     * yields {@code calcRate} and {@code calcPremium}. Any branch that still holds a regexp metacharacter after
     * the reduction fails the whole pattern.
     */
    static Set<String> convertRegexToGlobs(String regex) {
        if (regex == null || regex.isBlank()) {
            return Set.of();
        }
        regex = regex.trim();
        // Validate that the pattern is a valid regexp and can match a method signature
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            return Set.of();
        }
        var branches = expandAlternations(regex);
        if (branches.size() > MAX_ALTERNATION_BRANCHES) {
            // A pathological alternation (many nested groups) expands combinatorially; keep the filter rather
            // than spend unbounded time and memory unfolding it into globs.
            return Set.of();
        }
        var globs = new LinkedHashSet<String>();
        for (var branch : branches) {
            var glob = reduceSignatureToGlob(branch);
            if (glob == null) {
                return Set.of();
            }
            globs.add(glob);
        }
        return globs;
    }

    /**
     * Reduces one alternation-free signature regexp to a single name glob, or {@code null} when a regexp
     * metacharacter survives the reduction or the name reduces to nothing.
     */
    private static @Nullable String reduceSignatureToGlob(String regex) {
        var matcher = RETURN_TYPE_PREFIX.matcher(regex);
        if (matcher.find()) {
            // remove return type definition
            regex = matcher.replaceFirst("");
        } else if (!regex.matches("^[.][*+].*")) {
            // does not match to return type definition of the method signature
            return null;
        }
        // remove the (params) part of "<returnType> <methodName>(<params>)"
        var signatureMatcher = SIGNATURE_PARAMS.matcher(regex);
        if (signatureMatcher.find()) {
            regex = signatureMatcher.replaceFirst("");
        }
        // reduce the simple regexps left in the name part: .* and .+ to *, . to ?
        regex = regex.replace(".*", "*");
        regex = regex.replace(".+", "*");
        regex = regex.replace("?", "^"); // ? conflicts with the glob single-character wildcard
        regex = regex.replace(".", "?");
        for (var i = 0; i < regex.length(); i++) {
            var c = regex.charAt(i);
            if (c == '\\' || c == '[' || c == ']' || c == '(' || c == ')'
                    || c == '{' || c == '}' || c == '|' || c == '^'
                    || c == '+' || c == '.' || c == ' ') {
                return null;
            }
        }
        // An empty result is not a name glob (e.g. ".+ \(\)"); treat it as unconvertible so the filter is kept.
        return regex.isEmpty() ? null : regex;
    }

    /**
     * Expands a regexp's alternations into branches with none left: a top-level {@code sig1|sig2} is split
     * into separate signatures first, then each unescaped {@code (a|b)} group is unfolded by branch (its
     * parentheses removed). Splitting first avoids generating duplicate branches for a trailing signature.
     */
    private static List<String> expandAlternations(String regex) {
        var branches = new ArrayList<String>();
        for (var part : splitTopLevel(regex)) {
            branches.addAll(expandGroups(part));
        }
        return branches;
    }

    private static List<String> expandGroups(String regex) {
        var group = firstUnescapedGroup(regex);
        if (group == null) {
            return List.of(regex);
        }
        var prefix = regex.substring(0, group[0]);
        var content = regex.substring(group[0] + 1, group[1]);
        var suffix = regex.substring(group[1] + 1);
        var branches = new ArrayList<String>();
        for (var alternative : splitTopLevel(content)) {
            if (branches.size() > MAX_ALTERNATION_BRANCHES) {
                // Stop unfolding once the branch count is past the cap; the caller keeps the filter unchanged.
                break;
            }
            branches.addAll(expandGroups(prefix + alternative + suffix));
        }
        return branches;
    }

    /** The bounds of the first unescaped {@code (}…{@code )} group, or {@code null} when there is none. */
    private static int @Nullable [] firstUnescapedGroup(String regex) {
        var escaped = false;
        var open = -1;
        var depth = 0;
        for (var i = 0; i < regex.length(); i++) {
            var c = regex.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '(') {
                if (open < 0) {
                    open = i;
                }
                depth++;
            } else if (c == ')' && open >= 0 && --depth == 0) {
                return new int[]{open, i};
            }
        }
        return null;
    }

    /** Splits on every unescaped {@code |} that sits outside parentheses. */
    private static List<String> splitTopLevel(String regex) {
        var parts = new ArrayList<String>();
        var current = new StringBuilder();
        var depth = 0;
        var escaped = false;
        for (var i = 0; i < regex.length(); i++) {
            var c = regex.charAt(i);
            if (!escaped && c == '|' && depth == 0) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * Drops module configuration that only restates runtime defaults so the file shrinks to the minimal
     * form that produces the same behaviour: drops a module {@code <name>} equal to its workbook basename or
     * on a wildcard module, collapses same-folder nameless {@code .xlsx} modules into one
     * {@code <subfolder>/**}{@code /*.xlsx} wildcard, and drops the whole {@code <modules>} block when only
     * the default wildcards remain.
     *
     * <p>A module that carries its own configuration — a method filter or a {@code compileThisModuleOnly}
     * flag — is kept as its own entry, so the collapse never drops what a single module declared.
     *
     * <p>This transform derives each wildcard from the declared path alone; it does not read the folder, so
     * it can widen the module set when the folder holds undeclared workbooks. A caller that can see the
     * project files should guard against that with {@link #resolveModuleWorkbooks}.
     *
     * <p>Unlike the {@code openl:migrate} goal, this does not drop the project {@code <name>} — Studio keeps
     * whatever the file declares.
     */
    public static void defaultModules(ProjectDescriptor descriptor) {
        dropRedundantModuleNames(descriptor);
        collapseNamelessModulesToSubfolderWildcards(descriptor);
        dropModulesWhenAllAreDefaultWildcards(descriptor);
    }

    /**
     * The workbook files that become modules for the descriptor, resolved against the project's files.
     *
     * <p>A concrete module contributes its own path. A wildcard module contributes every workbook that
     * matches it — only real Excel workbooks, so non-Excel files and temporary {@code ~$} lock files are
     * ignored even when a pattern would match their name. A descriptor that declares no modules resolves
     * against the engine defaults ({@code rules/**}{@code /*.xlsx} and {@code tests/**}{@code /*.xlsx}).
     *
     * <p>Comparing this set before and after {@link #defaultModules}/{@link #apply} tells a caller whether a
     * migration would turn an undeclared workbook into a module — in any folder, {@code rules/},
     * {@code tests/} or another — so it can refuse the change.
     *
     * @param descriptor the descriptor to resolve
     * @param files      the project's file paths, relative to the project root and {@code /}-separated
     * @return the module workbook paths, {@code /}-separated
     */
    public static Set<String> resolveModuleWorkbooks(ProjectDescriptor descriptor, Collection<String> files) {
        var modules = descriptor.getModules();
        var effective = CollectionUtils.isEmpty(modules) ? ProjectDescriptor.defaultModules() : modules;
        var workbooks = files.stream()
                .map(path -> path.replace('\\', '/'))
                .filter(path -> FileTypeHelper.isExcelFile(FileUtils.getName(path)))
                .toList();
        var resolved = new LinkedHashSet<String>();
        for (var module : effective) {
            resolveModule(module, workbooks, resolved);
        }
        return resolved;
    }

    private static void resolveModule(Module module, List<String> files, Set<String> resolved) {
        var path = module.getRulesRootPath();
        if (path == null) {
            return;
        }
        if (module.isModuleWithWildcard()) {
            files.stream().filter(file -> FileUtils.pathMatches(path, file)).forEach(resolved::add);
        } else {
            resolved.add(path.replace('\\', '/'));
        }
    }

    /**
     * The workbook paths a migration would turn into modules — the sorted set difference of the module sets
     * {@link #resolveModuleWorkbooks} yields before and after the transform. Empty when the migration keeps
     * (or narrows) the module set; a non-empty result is a caller's signal to refuse the change.
     */
    public static List<String> addedWorkbooks(Set<String> before, Set<String> after) {
        return after.stream().filter(path -> !before.contains(path)).sorted().toList();
    }

    private static void dropRedundantModuleNames(ProjectDescriptor descriptor) {
        var modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            return;
        }
        for (var module : modules) {
            var rulesRootPath = module.getRulesRootPath();
            if (module.getName() == null || rulesRootPath == null) {
                continue;
            }
            if (module.isModuleWithWildcard() || module.getName().equals(FileUtils.getBaseName(rulesRootPath))) {
                module.setName(null);
            }
        }
    }

    private static void collapseNamelessModulesToSubfolderWildcards(ProjectDescriptor descriptor) {
        var modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            return;
        }
        var result = new ArrayList<Module>(modules.size());
        var covered = new HashSet<String>();
        var pending = new LinkedHashSet<String>();
        for (var m : modules) {
            var foldInto = foldableFolder(m);
            if (foldInto != null) {
                pending.add(foldInto);
                continue;
            }
            var path = m.getRulesRootPath();
            var seg = subfolder(path);
            // Only a config-free wildcard folds a folder into the recursive default. A wildcard that carries a
            // method filter or compileThisModuleOnly is kept as declared, so widening rules/*.xlsx to
            // rules/**/*.xlsx never applies that config to nested workbooks it did not cover.
            if (m.getName() == null && seg != null && path.endsWith(XLSX_EXT) && m.isModuleWithWildcard()
                    && hasNoExtraConfig(m)) {
                m.setRulesRootPath(seg + "/**/*.xlsx");
            }
            result.add(m);
            if (seg != null && m.isModuleWithWildcard() && hasNoExtraConfig(m)) {
                covered.add(seg);
            }
        }
        pending.removeAll(covered);
        pending.forEach(folder -> result.add(wildcardModule(folder)));
        descriptor.setModules(result);
    }

    /**
     * The subfolder a name-less concrete {@code .xlsx} module folds into, or {@code null} to keep the module
     * as its own entry. A module that carries its own configuration — a method filter or a
     * {@code compileThisModuleOnly} flag — is never folded, so collapsing a folder into one wildcard never
     * drops what a single module declared.
     */
    private static @Nullable String foldableFolder(Module m) {
        var path = m.getRulesRootPath();
        if (m.getName() == null && path != null && path.endsWith(XLSX_EXT)
                && !m.isModuleWithWildcard() && hasNoExtraConfig(m)) {
            return subfolder(path);
        }
        return null;
    }

    /** The leading path segment (the subfolder) of a module path, or {@code null} when there is none. */
    private static @Nullable String subfolder(@Nullable String path) {
        if (path == null) {
            return null;
        }
        var slash = path.indexOf('/');
        return slash <= 0 ? null : path.substring(0, slash);
    }

    private static Module wildcardModule(String folder) {
        var m = new Module();
        m.setRulesRootPath(folder + "/**/*.xlsx");
        return m;
    }

    private static void dropModulesWhenAllAreDefaultWildcards(ProjectDescriptor descriptor) {
        var modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            return;
        }
        var allDefaults = modules.stream().allMatch(m -> m.getName() == null
                && m.getRulesRootPath() != null
                && DEFAULT_WILDCARDS.contains(m.getRulesRootPath())
                && hasNoExtraConfig(m));
        if (allDefaults) {
            descriptor.setModules(null);
        }
    }

    private static boolean hasNoExtraConfig(Module m) {
        var ws = m.getWebstudioConfiguration();
        return m.getMethodFilter() == null && (ws == null || !ws.isCompileThisModuleOnly());
    }
}
