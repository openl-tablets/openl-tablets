package org.openl.studio.projects.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openl.rules.project.model.Module;
import org.openl.studio.projects.model.ModuleViewModel;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * The modules of a project as the screen shows them: what {@code rules.xml} declares, and what each
 * declaration stands for.
 *
 * <p>A declaration whose path is a pattern resolves to one module per matching file. The screen shows
 * the declaration itself, with those modules under it, so the list is as long as the file is and a
 * pattern that matched nothing is still visible.
 */
final class ProjectModules {

    private ProjectModules() {
    }

    /**
     * Maps the declared modules onto the modules they resolved to.
     *
     * @param declared modules as {@code rules.xml} declares them
     * @param resolved modules the project resolved to, each pattern already replaced by its matches
     */
    static List<ModuleViewModel> map(List<Module> declared, List<Module> resolved) {
        var matched = matchesByPattern(resolved);
        var views = new ArrayList<ModuleViewModel>(declared.size());
        for (var module : declared) {
            if (module.isModuleWithWildcard()) {
                var pattern = module.getRulesRootPath();
                views.add(ModuleViewModel.pattern(module.getName(), pattern, matched.getOrDefault(pattern, List.of())));
            } else {
                views.add(module(module));
            }
        }
        declaredPathsAside(declared, resolved).forEach(module -> views.add(module(module)));
        return views;
    }

    /** The matches of every pattern, in the order the project resolved them. */
    private static Map<String, List<ModuleViewModel>> matchesByPattern(List<Module> resolved) {
        var matched = new LinkedHashMap<String, List<ModuleViewModel>>();
        for (var module : resolved) {
            var pattern = module.getWildcardRulesRootPath();
            if (pattern != null) {
                matched.computeIfAbsent(pattern, key -> new ArrayList<>()).add(module(module));
            }
        }
        return matched;
    }

    /**
     * The modules no declaration accounts for. A project without {@code rules.xml} is made of the Excel
     * files found in it, which are modules all the same.
     */
    private static List<Module> declaredPathsAside(List<Module> declared, List<Module> resolved) {
        var declaredPaths = declared.stream().map(Module::getRulesRootPath).collect(Collectors.toSet());
        return resolved.stream()
                .filter(module -> module.getWildcardRulesRootPath() == null)
                .filter(module -> !declaredPaths.contains(module.getRulesRootPath()))
                .toList();
    }

    /** A module reads by its name; a declaration that leaves the name out is named after its file. */
    private static ModuleViewModel module(Module module) {
        var path = module.getRulesRootPath();
        var name = StringUtils.isNotBlank(module.getName()) || path == null
                ? module.getName()
                : FileUtils.getBaseName(path);
        return ModuleViewModel.module(name, path);
    }
}
