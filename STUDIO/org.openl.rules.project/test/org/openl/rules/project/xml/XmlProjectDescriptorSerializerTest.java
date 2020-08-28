package org.openl.rules.project.xml;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.Property;
import org.openl.util.IOUtils;

public class XmlProjectDescriptorSerializerTest {

    @Test
    public void testSerialize() throws FileNotFoundException {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
                .deserialize(new FileInputStream("test-resources/xml/rules1.xml"));

        String newRulesXML = new XmlProjectDescriptorSerializer().serialize(pd);
        ProjectDescriptor pd1 = new XmlProjectDescriptorSerializer().deserialize(IOUtils.toInputStream(newRulesXML));

        assertEquals(2, pd1.getProperties().size());
        assertEquals("name1", pd1.getProperties().get(0).getName());
        assertEquals("value2", pd1.getProperties().get(1).getValue());
        assertArrayEquals(new String[] {"properties-file-name-pattern"}, pd1.getPropertiesFileNamePatterns());
        assertEquals("properties-file-name-processor", pd1.getPropertiesFileNameProcessor());
    }

    @Test
    public void testDeserialize() throws FileNotFoundException {
        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
            .deserialize(new FileInputStream("test-resources/xml/rules1.xml"));

        assertEquals(2, pd.getProperties().size());
        Property p = pd.getProperties().get(0);
        assertEquals("name1", p.getName());
        assertEquals("value2", pd.getProperties().get(1).getValue());
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
