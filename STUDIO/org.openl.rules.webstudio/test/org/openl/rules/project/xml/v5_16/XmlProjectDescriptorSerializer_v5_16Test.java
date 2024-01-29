package org.openl.rules.project.xml.v5_16;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12Test.PROJECT_DESCRIPTOR_V5_12_XML;
import static org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12Test.assertProjectDescriptorDeserializedCorrectly;

import java.io.FileInputStream;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_16.ProjectDescriptor_v5_16;
import org.openl.rules.project.model.v5_16.converter.ProjectDescriptorVersionConverter_5_16;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializerTest;

public class XmlProjectDescriptorSerializer_v5_16Test {

    @Test
    public void testSerialize() throws Exception {
        ProjectDescriptor projectDescriptor = BaseProjectDescriptorSerializerTest.initProjectDescriptorForTest();

        String xml = new BaseProjectDescriptorSerializer<>(
                new ProjectDescriptorVersionConverter_5_16(), ProjectDescriptor_v5_16.class)
            .serialize(projectDescriptor);
        assertEquals(xml, PROJECT_DESCRIPTOR_V5_12_XML);
    }

    @Test
    public void testDeserialize()  throws Exception {
        ProjectDescriptor deserializedProject = new BaseProjectDescriptorSerializer<>(
                new ProjectDescriptorVersionConverter_5_16(), ProjectDescriptor_v5_16.class)
            .deserialize(
                new FileInputStream("test-resources/org.openl.rules.project.xml/project-descriptor_v5_12.xml"));
        assertProjectDescriptorDeserializedCorrectly(deserializedProject);
    }
}

