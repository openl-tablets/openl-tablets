package org.openl.rules.project.xml.v5_13;

import org.junit.Test;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializerTest;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12Test.PROJECT_DESCRIPTOR_V5_12_XML;
import static org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12Test.assertProjectDescriptorDeserializedCorrectly;

public class XmlProjectDescriptorSerializer_v5_13Test {

    @Test
    public void testSerialize() {
        ProjectDescriptor projectDescriptor = BaseProjectDescriptorSerializerTest.initProjectDescriptorForTest();

        String xml = new XmlProjectDescriptorSerializer_v5_13().serialize(projectDescriptor);
        assertEquals(xml, PROJECT_DESCRIPTOR_V5_12_XML);
    }

    @Test
    public void testDeserialize()  throws IOException {
        ProjectDescriptor deserializedProject = new XmlProjectDescriptorSerializer_v5_13().deserialize(
                new FileInputStream("test-resources/org.openl.rules.project.xml/project-descriptor_v5_12.xml"));
        assertProjectDescriptorDeserializedCorrectly(deserializedProject);
    }
}

