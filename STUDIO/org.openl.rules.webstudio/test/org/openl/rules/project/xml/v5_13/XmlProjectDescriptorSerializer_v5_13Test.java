package org.openl.rules.project.xml.v5_13;

import org.junit.Test;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_13.ProjectDescriptor_v5_13;
import org.openl.rules.project.model.v5_13.converter.ProjectDescriptor_v5_13VersionConverter;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializerTest;

import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12Test.PROJECT_DESCRIPTOR_V5_12_XML;
import static org.openl.rules.project.xml.v5_12.XmlProjectDescriptorSerializer_v5_12Test.assertProjectDescriptorDeserializedCorrectly;

public class XmlProjectDescriptorSerializer_v5_13Test {

    @Test
    public void testSerialize() throws Exception {
        ProjectDescriptor projectDescriptor = BaseProjectDescriptorSerializerTest.initProjectDescriptorForTest();

        String xml = new BaseProjectDescriptorSerializer<>(
                new ProjectDescriptor_v5_13VersionConverter(), ProjectDescriptor_v5_13.class)
                .serialize(projectDescriptor);
        assertEquals(xml, PROJECT_DESCRIPTOR_V5_12_XML);
    }

    @Test
    public void testDeserialize()  throws Exception {
        ProjectDescriptor deserializedProject = new BaseProjectDescriptorSerializer<>(
                new ProjectDescriptor_v5_13VersionConverter(), ProjectDescriptor_v5_13.class)
            .deserialize(
                new FileInputStream("test-resources/org.openl.rules.project.xml/project-descriptor_v5_12.xml"));
        assertProjectDescriptorDeserializedCorrectly(deserializedProject);
    }
}

