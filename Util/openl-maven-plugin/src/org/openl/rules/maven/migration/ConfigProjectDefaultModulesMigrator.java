package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;

/**
 * {@code rules.xml} migration: drops module and project-name configuration that only restates runtime
 * defaults so the file shrinks to the minimal form that produces the same behaviour. Applies the
 * following idempotent rules in order:
 * <ol>
 *     <li>Drop a module {@code <name>} that equals the basename of its {@code <rules-root>} path or
 *         whose path contains a wildcard ({@code *} or {@code ?}) — the OpenL resolver derives the same
 *         name from the path in either case.</li>
 *     <li>For every name-less {@code .xlsx} module with a top-level subfolder, replace it with a
 *         {@code <subfolder>}{@code /**}{@code /*.xlsx} wildcard:
 *         <ul>
 *             <li>An anonymous wildcard (e.g. {@code rules/*.xlsx}) is normalised in place so its
 *                 position is preserved.</li>
 *             <li>An anonymous non-wildcard (e.g. {@code rules/A.xlsx}) is removed from its position;
 *                 a single {@code <subfolder>}{@code /**}{@code /*.xlsx} is appended at the end per
 *                 folder, unless that folder is already covered by a wildcard in the result.</li>
 *         </ul>
 *         Named modules, root-level paths, and non-{@code .xlsx} entries pass through untouched.</li>
 *     <li>If after the collapse every remaining module is a default wildcard
 *         ({@code rules/**}{@code /*.xlsx} and/or {@code tests/**}{@code /*.xlsx}), drop the
 *         {@code <modules>} block entirely — the resolver falls back to the same wildcard patterns.</li>
 *     <li>Drop the project {@code <name>} when it equals the folder containing {@code rules.xml} — the
 *         resolver derives the same name from the folder.</li>
 * </ol>
 * <p>
 * Migrator id: {@code config.project.default-modules}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectDefaultModulesMigrator implements Migrator {

    private static final String XLSX_EXT = ".xlsx";
    private static final Set<String> DEFAULT_WILDCARDS = Set.of("rules/**/*.xlsx", "tests/**/*.xlsx");

    /**
     * Package-private for direct unit testing.
     */
    static void transform(ProjectDescriptor descriptor) {
        dropRedundantModuleNames(descriptor);
        collapseNamelessModulesToSubfolderWildcards(descriptor);
        dropModulesWhenAllAreDefaultWildcards(descriptor);
        dropProjectNameWhenEqualsFolder(descriptor);
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
            var path = m.getRulesRootPath();
            int slash = path.indexOf('/');
            var seg = slash <= 0 ? null : path.substring(0, slash);
            if (m.getName() == null && seg != null && path.endsWith(XLSX_EXT)) {
                if (!m.isModuleWithWildcard()) {
                    pending.add(seg);
                    continue;
                }
                m.setRulesRootPath(seg + "/**/*.xlsx");
            }
            result.add(m);
            if (m.isModuleWithWildcard() && seg != null) {
                covered.add(seg);
            }
        }
        pending.removeAll(covered);
        pending.forEach(folder -> result.add(wildcardModule(folder)));
        descriptor.setModules(result);
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
        boolean allDefaults = modules.stream().allMatch(m -> m.getName() == null
                && m.getRulesRootPath() != null
                && DEFAULT_WILDCARDS.contains(m.getRulesRootPath())
                && hasNoExtraConfig(m));
        if (allDefaults) {
            descriptor.setModules(null);
        }
    }

    /**
     * {@code true} when the module carries no configuration beyond {@code name} and {@code rules-root}.
     * Guards rule 3 against silently discarding {@code <method-filter>} or non-default
     * {@code <webstudioConfiguration>} settings when the module is otherwise a default wildcard.
     */
    private static boolean hasNoExtraConfig(Module m) {
        var ws = m.getWebstudioConfiguration();
        return m.getMethodFilter() == null && (ws == null || !ws.isCompileThisModuleOnly());
    }

    private static void dropProjectNameWhenEqualsFolder(ProjectDescriptor descriptor) {
        var projectFolder = descriptor.getProjectFolder();
        if (projectFolder == null) {
            return;
        }
        var folderName = projectFolder.getFileName();
        if (folderName != null && folderName.toString().equals(descriptor.getName())) {
            descriptor.setName(null);
        }
    }

    @Override
    public String getId() {
        return "config.project.default-modules";
    }

    @Override
    public String getCommitMessage() {
        return "drop redundant module and project-name defaults from rules.xml";
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface) throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder, ConfigProjectDefaultModulesMigrator::transform);
    }
}
