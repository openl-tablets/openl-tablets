package org.openl.rules.project.xml;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.project.model.*;
import org.openl.rules.project.model.Module;
import org.openl.util.IOUtils;

public class XmlProjectDescriptorSerializerTest {

    @Test
    public void testSerialize() {
        ProjectDescriptor projectDescriptor = initProjectDescriptorForTest();

        XmlProjectDescriptorSerializer descriptorSerializer = new XmlProjectDescriptorSerializer();
        String xml = descriptorSerializer.serialize(projectDescriptor);
        assertEquals(xml, xml);
    }

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

        projectDescriptor.setPropertiesFileNamePatterns(new String[] {" {lob}"});
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

    @Test
    public void testDeserialize() throws FileNotFoundException {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
            .deserialize(new FileInputStream("test-resources/xml/rules1.xml"));

        assertArrayEquals(new String[] {"properties-file-name-pattern"}, pd.getPropertiesFileNamePatterns());
        assertEquals("properties-file-name-processor", pd.getPropertiesFileNameProcessor());
    }

    @Test
    public void testDeserializeMultiPattern() throws FileNotFoundException {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/multi-file-name-pattern/rules.xml"));

        assertEquals("test ?", pd.getName());
        List<Module> modules = pd.getModules();
        assertNotNull(modules);
        assertEquals(1, modules.size());
        Module m = modules.get(0);
        assertEquals("testmodule", m.getName());
        assertEquals("dependencies/test3/module/dependency-module?/dependency?.xls", m.getRulesRootPath().getPath());
        assertArrayEquals(new String[] {"%lob%-%usState%", "Tests-*", "DataTables"}, pd.getPropertiesFileNamePatterns());
    }

    @Test
    public void testSerializeMultiPattern() throws FileNotFoundException {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/multi-file-name-pattern/rules.xml"));

        String newRulesXML = new XmlProjectDescriptorSerializer().serialize(pd);
        ProjectDescriptor pd1 = new XmlProjectDescriptorSerializer().deserialize(IOUtils.toInputStream(newRulesXML));

        assertEquals("test ?", pd1.getName());
        List<Module> modules = pd1.getModules();
        assertNotNull(modules);
        assertEquals(1, modules.size());
        Module m = modules.get(0);
        assertEquals("testmodule", m.getName());
        assertEquals("dependencies/test3/module/dependency-module?/dependency?.xls", m.getRulesRootPath().getPath());
        assertArrayEquals(new String[] {"%lob%-%usState%", "Tests-*", "DataTables"}, pd1.getPropertiesFileNamePatterns());
    }

}
