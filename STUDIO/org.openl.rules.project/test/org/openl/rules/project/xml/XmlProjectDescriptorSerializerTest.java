package org.openl.rules.project.xml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.Property;
import org.openl.util.IOUtils;

public class XmlProjectDescriptorSerializerTest {

    @Test
    public void testSerialize() {
        ProjectDescriptor pd = null;
        try {
            pd = new XmlProjectDescriptorSerializer().deserialize(new FileInputStream("test-resources/xml/rules1.xml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String newRulesXML = new XmlProjectDescriptorSerializer().serialize(pd);

        ProjectDescriptor pd1 = new XmlProjectDescriptorSerializer().deserialize(IOUtils.toInputStream(newRulesXML));

        Assert.assertEquals(2, pd1.getProperties().size());

        Assert.assertEquals("name1", pd1.getProperties().get(0).getName());
        Assert.assertEquals("value2", pd1.getProperties().get(1).getValue());

        Assert.assertEquals("properties-file-name-pattern", pd.getPropertiesFileNamePattern());
        Assert.assertEquals("properties-file-name-processor", pd.getPropertiesFileNameProcessor());
    }

    @Test
    public void testDeserialize() throws FileNotFoundException {

        ProjectDescriptor pd = new XmlProjectDescriptorSerializer()
            .deserialize(new FileInputStream("test-resources/xml/rules1.xml"));

        Assert.assertEquals(2, pd.getProperties().size());

        Property p = pd.getProperties().get(0);
        Assert.assertEquals("name1", p.getName());
        Assert.assertEquals("value2", pd.getProperties().get(1).getValue());

        Assert.assertEquals("properties-file-name-pattern", pd.getPropertiesFileNamePattern());
        Assert.assertEquals("properties-file-name-processor", pd.getPropertiesFileNameProcessor());

    }

}
