package org.openl.rules.project.xml;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseProjectDescriptorSerializerTest {

    public static ProjectDescriptor initProjectDescriptorForTest() {
        ProjectDescriptor projectDescriptor = new ProjectDescriptor();
        projectDescriptor.setId("projectId ");
        projectDescriptor.setName("projectDescriptor Name ");
        projectDescriptor.setComment("projectDescriptor Comment ");
        projectDescriptor.setProjectFolder(Paths.get("path "));

        projectDescriptor.setModules(List.of(initModuleForTest(" 1"), initModuleForTest(" 2")));
        projectDescriptor.setClasspath(List.of(new PathEntry(" rules2/*.xlsx"), new PathEntry(" rules3/*.xlsx")));

        projectDescriptor.setOpenapi(new OpenAPI("openAPIPathParam", OpenAPI.Mode.RECONCILIATION, null, null));

        ProjectDependencyDescriptor descriptor = new ProjectDependencyDescriptor();
        descriptor.setName("ProjectDependencyDescriptor Name ");
        descriptor.setAutoIncluded(true);
        projectDescriptor.setDependencies(List.of(descriptor));

        projectDescriptor.setPropertiesFileNamePatterns(new String[]{" {lob}"});
        projectDescriptor.setPropertiesFileNameProcessor(" default.DefaultProcessor");

        return projectDescriptor;
    }

    public static Module initModuleForTest(String tag) {
        Module module = new Module();
        module.setName(" Module Name" + tag);
        module.setRulesRootPath(new PathEntry(tag + "rules/*.xlsx"));

        ProjectDescriptor moduleProject = new ProjectDescriptor();
        moduleProject.setName(" module project" + tag);
        module.setProject(moduleProject);
        Map<String, Object> properties = new HashMap<>();
        properties.put(" prop1" + tag, 1);
        properties.put(" prop2" + tag, "yellow" + tag);
        module.setProperties(properties);
        module.setWildcardName(" wild card name" + tag);
        module.setWildcardRulesRootPath(" wildCardRulesRootPath" + tag);

        MethodFilter methodFilter = new MethodFilter();
        methodFilter.addExcludePattern("^test" + tag, "\\d");
        methodFilter.addIncludePattern(" * ");
        module.setMethodFilter(methodFilter);

        return module;
    }

    private static final BaseProjectDescriptorSerializer.CollapsedStringAdapter2 collapsedStringAdapter2 = new BaseProjectDescriptorSerializer.CollapsedStringAdapter2();

    public static String collapseExtraWhitespaces(String input) {
        return collapsedStringAdapter2.marshal(input);
    }

    public static Collection<String> collapseExtraWhitespaces(Collection<String> collection) {
        return collection.stream().map(BaseProjectDescriptorSerializerTest::collapseExtraWhitespaces).collect(Collectors.toList());
    }
}
