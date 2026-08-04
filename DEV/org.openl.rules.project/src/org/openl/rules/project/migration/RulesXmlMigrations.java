package org.openl.rules.project.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.CollectionUtils;
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
     * Each include and exclude regexp is converted to an exposed-methods glob, the results merge with any
     * existing {@code <exposed-methods>}, and the module-level filters are removed.
     *
     * <p>A regexp that does not convert to a clean glob is dropped, so a project that used only such patterns
     * gets no {@code <exposed-methods>} and exposes every method. An already-declared {@code <exposed-methods>}
     * is preserved and extended, never replaced.
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
        for (var module : modules) {
            var filter = module.getMethodFilter();
            if (filter == null) {
                continue;
            }
            collectGlobs(filter.getIncludes(), includes);
            collectGlobs(filter.getExcludes(), excludes);
            module.setMethodFilter(null);
        }
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

    private static void collectGlobs(Set<String> patterns, Set<String> target) {
        if (patterns == null) {
            return;
        }
        for (var pattern : patterns) {
            var glob = convertRegexToGlob(pattern);
            if (StringUtils.isNotBlank(glob)) {
                target.add(glob);
            }
        }
    }

    private static void addAll(Set<String> source, Set<String> target) {
        if (source != null) {
            target.addAll(source);
        }
    }

    /**
     * Converts a legacy method-filter regexp (matched against a full method signature) to an exposed-methods
     * glob (matched against the method name only). Returns {@code null} when the pattern is not a valid regexp
     * or cannot be reduced to a clean glob, so the caller drops it.
     *
     * <p>The regexp is matched against a {@code returnType methodName(argType1, argTypeN)} signature. Common
     * shapes reduce as {@code .+ methodName\(.+\)} to {@code methodName}, {@code .*} or {@code .+} to the bare
     * {@code *}, and {@code .*methodName.*} to {@code *methodName*}. Any pattern that still holds a regexp
     * metacharacter after the reduction is rejected.
     */
    static String convertRegexToGlob(String regex) {
        if (regex == null || regex.isBlank()) {
            return null;
        }
        regex = regex.trim();

        // Validate that the pattern is a valid regexp and can match a method signature
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            // Not a valid regex
            return null;
        }

        var prefix = Pattern.compile("^[^ (]+ ");
        var matcher = prefix.matcher(regex);
        if (matcher.find()) {
            // remove return type definition
            regex = matcher.replaceFirst("");
        } else if (!regex.matches("^[.][*+].*")) {
            // does not match to return type definition of the method signature
            return null;
        }

        // Pattern: <returnType> <methodName>(<params>)
        // e.g., ".+ methodName\(.+\)" or ".* methodName\(.*\)" or ".+ methodName\(\)"
        var signaturePattern = Pattern.compile("\\\\\\(.*\\\\\\)$");
        var signatureMatcher = signaturePattern.matcher(regex);
        if (signatureMatcher.find()) {
            regex = signatureMatcher.replaceFirst("");
        }

        // Try to convert simple regex patterns in the name part to glob
        // Replace .* and .+ with glob *, and . with ?
        regex = regex.replace("(.*)", "*");
        regex = regex.replace("(.+)", "*");
        regex = regex.replace(".*", "*");
        regex = regex.replace(".+", "*");
        regex = regex.replace("?", "^"); // replace on the illegal symbol due conflict with Glob
        regex = regex.replace(".", "?");

        // check on the illegal symbols in the method name glob
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\' || c == '[' || c == ']' || c == '(' || c == ')'
                    || c == '{' || c == '}' || c == '|' || c == '^'
                    || c == '+' || c == '.' || c == ' ') {
                return null;
            }
        }
        // If the result looks clean (no remaining regex metacharacters), return it
        return regex;
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
     * matches it. A descriptor that declares no modules resolves against the engine defaults
     * ({@code rules/**}{@code /*.xlsx} and {@code tests/**}{@code /*.xlsx}).
     *
     * <p>Comparing this set before and after {@link #defaultModules}/{@link #apply} tells a caller whether a
     * migration would turn an undeclared workbook into a module — in any folder, {@code rules/},
     * {@code tests/} or another — so it can refuse the change.
     *
     * @param descriptor    the descriptor to resolve
     * @param workbookPaths the project's workbook paths, relative to the project root and {@code /}-separated
     * @return the module workbook paths, {@code /}-separated
     */
    public static Set<String> resolveModuleWorkbooks(ProjectDescriptor descriptor, Collection<String> workbookPaths) {
        var modules = descriptor.getModules();
        var effective = CollectionUtils.isEmpty(modules) ? ProjectDescriptor.defaultModules() : modules;
        var resolved = new LinkedHashSet<String>();
        for (var module : effective) {
            resolveModule(module, workbookPaths, resolved);
        }
        return resolved;
    }

    private static void resolveModule(Module module, Collection<String> workbookPaths, Set<String> resolved) {
        var path = module.getRulesRootPath();
        if (path == null) {
            return;
        }
        if (module.isModuleWithWildcard()) {
            workbookPaths.stream()
                    .filter(workbook -> FileUtils.pathMatches(path, workbook.replace('\\', '/')))
                    .map(workbook -> workbook.replace('\\', '/'))
                    .forEach(resolved::add);
        } else {
            resolved.add(path.replace('\\', '/'));
        }
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
            if (m.getName() == null && seg != null && path.endsWith(XLSX_EXT) && m.isModuleWithWildcard()) {
                m.setRulesRootPath(seg + "/**/*.xlsx");
            }
            result.add(m);
            if (seg != null && m.isModuleWithWildcard()) {
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
