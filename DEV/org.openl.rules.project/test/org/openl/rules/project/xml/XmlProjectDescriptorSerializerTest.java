package org.openl.rules.project.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.IOUtils;

public class XmlProjectDescriptorSerializerTest {

    @Test
    public void testSerialize() throws Exception {
        ProjectDescriptor projectDescriptor = initProjectDescriptorForTest();

        XmlProjectDescriptorSerializer descriptorSerializer = new XmlProjectDescriptorSerializer();
        String xml = descriptorSerializer.serialize(projectDescriptor);
        assertEquals(xml, xml);
    }

    public static ProjectDescriptor initProjectDescriptorForTest() {
        ProjectDescriptor projectDescriptor = new ProjectDescriptor();
        projectDescriptor.setName("projectDescriptor Name ");
        projectDescriptor.setComment("projectDescriptor Comment ");

        projectDescriptor.setProjectFolder(Path.of("path"));

        projectDescriptor.setModules(List.of(initModuleForTest(" 1"), initModuleForTest(" 2")));
        projectDescriptor.setClasspath(List.of(new PathEntry(" rules2/*.xlsx"), new PathEntry(" rules3/*.xlsx")));

        projectDescriptor.setOpenapi(new OpenAPI("openAPIPathParam", OpenAPI.Mode.RECONCILIATION, null, null));

        ProjectDependencyDescriptor descriptor = new ProjectDependencyDescriptor();
        descriptor.setName("ProjectDependencyDescriptor Name ");
        descriptor.setAutoIncluded(true);
        projectDescriptor.setDependencies(List.of(descriptor));

        projectDescriptor.setPropertiesFileNamePatterns(new String[]{" {lob}"});
        projectDescriptor.setPropertiesFileNameProcessor(" default.DefaultProcessor");

        var interfaceMethods = new MethodFilter();
        interfaceMethods.addIncludePattern("get*", "calculate*");
        interfaceMethods.addExcludePattern("internal*");
        projectDescriptor.setInterfaceMethods(interfaceMethods);

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
    public void testDeserialize() throws Exception {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/xml/rules1.xml"));

        assertArrayEquals(new String[]{"properties-file-name-pattern"}, pd.getPropertiesFileNamePatterns());
        assertEquals("properties-file-name-processor", pd.getPropertiesFileNameProcessor());
    }

    @Test
    public void testDeserializeMultiPattern() throws Exception {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/multi-file-name-pattern/rules.xml"));

        assertEquals("test ?", pd.getName());
        List<Module> modules = pd.getModules();
        assertNotNull(modules);
        assertEquals(1, modules.size());
        Module m = modules.getFirst();
        assertEquals("testmodule", m.getName());
        assertEquals("dependencies/test3/module/dependency-module?/dependency?.xls", m.getRulesRootPath().getPath());
        assertArrayEquals(new String[]{"%lob%-%usState%", "Tests-*", "DataTables"}, pd.getPropertiesFileNamePatterns());
    }

    @Test
    public void testDeserializeInterfaceMethods() throws Exception {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/xml/rules-with-interface-methods.xml"));

        assertNotNull(pd.getInterfaceMethods());
        assertNotNull(pd.getInterfaceMethods().getIncludes());
        assertEquals(2, pd.getInterfaceMethods().getIncludes().size());
        assertTrue(pd.getInterfaceMethods().getIncludes().contains("get*"));
        assertTrue(pd.getInterfaceMethods().getIncludes().contains("calculatePremium"));
        assertNotNull(pd.getInterfaceMethods().getExcludes());
        assertEquals(1, pd.getInterfaceMethods().getExcludes().size());
        assertTrue(pd.getInterfaceMethods().getExcludes().contains("internal*"));
    }

    @Test
    public void testDeserializeWithoutInterfaceMethods() throws Exception {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/xml/rules1.xml"));

        assertNull(pd.getInterfaceMethods());
    }

    @Test
    public void testSerializeInterfaceMethodsRoundTrip() throws Exception {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/xml/rules-with-interface-methods.xml"));

        String xml = new XmlProjectDescriptorSerializer().serialize(pd);
        ProjectDescriptor pd2 = new XmlProjectDescriptorSerializer().deserialize(IOUtils.toInputStream(xml));

        assertNotNull(pd2.getInterfaceMethods());
        assertEquals(pd.getInterfaceMethods().getIncludes().size(), pd2.getInterfaceMethods().getIncludes().size());
        assertTrue(pd2.getInterfaceMethods().getIncludes().contains("get*"));
        assertTrue(pd2.getInterfaceMethods().getIncludes().contains("calculatePremium"));
        assertEquals(pd.getInterfaceMethods().getExcludes().size(), pd2.getInterfaceMethods().getExcludes().size());
        assertTrue(pd2.getInterfaceMethods().getExcludes().contains("internal*"));
    }

    @Test
    public void testSerializeMultiPattern() throws Exception {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/multi-file-name-pattern/rules.xml"));

        String newRulesXML = new XmlProjectDescriptorSerializer().serialize(pd);
        ProjectDescriptor pd1 = new XmlProjectDescriptorSerializer().deserialize(IOUtils.toInputStream(newRulesXML));

        assertEquals("test ?", pd1.getName());
        List<Module> modules = pd1.getModules();
        assertNotNull(modules);
        assertEquals(1, modules.size());
        Module m = modules.getFirst();
        assertEquals("testmodule", m.getName());
        assertEquals("dependencies/test3/module/dependency-module?/dependency?.xls", m.getRulesRootPath().getPath());
        assertArrayEquals(new String[]{"%lob%-%usState%", "Tests-*", "DataTables"}, pd1.getPropertiesFileNamePatterns());
    }

}
