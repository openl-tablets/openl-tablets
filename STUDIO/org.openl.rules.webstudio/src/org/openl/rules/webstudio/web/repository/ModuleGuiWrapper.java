package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

public class ModuleGuiWrapper {
    private static final String SEPARATOR_PATTERN = "[,;\\s]+";
    private static final String SEPARATOR = ", ";

    private final Module module;

    public static List<ModuleGuiWrapper> wrap(List<Module> modules) {
        List<ModuleGuiWrapper> list = new ArrayList<ModuleGuiWrapper>();
        for (Module module : modules) {
            list.add(new ModuleGuiWrapper(module));
        }
        return list;
    }

    public ModuleGuiWrapper(Module module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public String getIncludedMethods() {
        return StringUtils.join(module.getMethodFilter().getIncludes(), SEPARATOR);
    }

    public void setIncludedMethods(String methods) {
        Set<String> includes = StringUtils.isNotBlank(methods) ?
                new HashSet<String>(Arrays.asList(methods.split(SEPARATOR_PATTERN))) : null;
        module.getMethodFilter().setIncludes(includes);
    }

    public String getExcludedMethods() {
        return StringUtils.join(module.getMethodFilter().getExcludes(), SEPARATOR);
    }

    public void setExcludedMethods(String methods) {
        Set<String> excludes = StringUtils.isNotBlank(methods) ?
                new HashSet<String>(Arrays.asList(methods.split(SEPARATOR_PATTERN))) : null;
        module.getMethodFilter().setExcludes(excludes);
    }

    public PathEntry getRulesRootPath() {
        return module.getRulesRootPath();
    }

    public void setRulesRootPath(PathEntry rulesRootPath) {
        module.setRulesRootPath(rulesRootPath);
    }

    public MethodFilter getMethodFilter() {
        return module.getMethodFilter();
    }

    public void setMethodFilter(MethodFilter methodFilter) {
        module.setMethodFilter(methodFilter);
    }

    public ProjectDescriptor getProject() {
        return module.getProject();
    }

    public void setProject(ProjectDescriptor project) {
        module.setProject(project);
    }

    public String getName() {
        return module.getName();
    }

    public void setName(String name) {
        module.setName(name);
    }

    public ModuleType getType() {
        return module.getType();
    }

    public void setType(ModuleType type) {
        module.setType(type);
        if (ModuleType.API == type) {
            setClassname(null);
        }
    }

    public String getClassname() {
        return module.getClassname();
    }

    public void setClassname(String classname) {
        module.setClassname(classname);
    }

    public Map<String, Object> getProperties() {
        return module.getProperties();
    }

    public void setProperties(Map<String, Object> properties) {
        module.setProperties(properties);
    }

}
