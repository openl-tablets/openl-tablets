package org.openl.rules.project.xml.v5_11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.project.xml.BaseProjectDescriptorSerializerTest.collapseExtraWhitespaces;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_11.ProjectDescriptor_v5_11;
import org.openl.rules.project.model.v5_11.converter.ProjectDescriptorVersionConverter_v5_11;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializerTest;

public class XmlProjectDescriptorSerializer_v5_11Test {

    @Test
    public void testSerialize() throws Exception {
        ProjectDescriptor projectDescriptor = BaseProjectDescriptorSerializerTest.initProjectDescriptorForTest();

        BaseProjectDescriptorSerializer<ProjectDescriptor_v5_11> descriptorSerializer_v5_11 =
                new BaseProjectDescriptorSerializer<>(new ProjectDescriptorVersionConverter_v5_11(), ProjectDescriptor_v5_11.class);
        String xml = descriptorSerializer_v5_11.serialize(projectDescriptor);
        assertEquals(xml, PROJECT_DESCRIPTOR_V5_11_XML);
    }

    private static final String PROJECT_DESCRIPTOR_V5_11_XML = "<project>\n" +
            "    <id>projectDescriptor Name</id>\n" +
            "    <name>projectDescriptor Name</name>\n" +
            "    <comment>projectDescriptor Comment</comment>\n" +
            "    <modules>\n" +
            "        <module>\n" +
            "            <name>Module Name 1</name>\n" +
            "            <rules-root path=\"1rules/*.xlsx\"/>\n" +
            "            <method-filter>\n" +
            "                <includes>\n" +
            "                    <value>*</value>\n" +
            "                </includes>\n" +
            "                <excludes>\n" +
            "                    <value>^test 1</value>\n" +
            "                    <value>\\d</value>\n" +
            "                </excludes>\n" +
            "            </method-filter>\n" +
            "        </module>\n" +
            "        <module>\n" +
            "            <name>Module Name 2</name>\n" +
            "            <rules-root path=\"2rules/*.xlsx\"/>\n" +
            "            <method-filter>\n" +
            "                <includes>\n" +
            "                    <value>*</value>\n" +
            "                </includes>\n" +
            "                <excludes>\n" +
            "                    <value>^test 2</value>\n" +
            "                    <value>\\d</value>\n" +
            "                </excludes>\n" +
            "            </method-filter>\n" +
            "        </module>\n" +
            "    </modules>\n" +
            "    <classpath>\n" +
            "        <entry path=\"rules2/*.xlsx\"/>\n" +
            "        <entry path=\"rules3/*.xlsx\"/>\n" +
            "    </classpath>\n" +
            "</project>";

    @Test
    public void testDeserialize() throws Exception {
        BaseProjectDescriptorSerializer<ProjectDescriptor_v5_11> descriptorSerializer_v5_11 =
                new BaseProjectDescriptorSerializer<>(
                        new ProjectDescriptorVersionConverter_v5_11(), ProjectDescriptor_v5_11.class);
        ProjectDescriptor deserializedProject = descriptorSerializer_v5_11.deserialize(
                new FileInputStream("test-resources/org.openl.rules.project.xml/project-descriptor_v5_11.xml"));
        ProjectDescriptor project = BaseProjectDescriptorSerializerTest.initProjectDescriptorForTest();

        assertNull(deserializedProject.getId());
        assertEquals(deserializedProject.getName(), collapseExtraWhitespaces(project.getName()));
        assertEquals(deserializedProject.getComment(), collapseExtraWhitespaces(project.getComment()));
        assertEquals(deserializedProject.getClasspath().size(), project.getClasspath().size());
        assertEquals(deserializedProject.getClasspath().size(), project.getClasspath().size());

        assertEquals(deserializedProject.getClasspath().stream().map(PathEntry::getPath).collect(Collectors.toList()),
                collapseExtraWhitespaces(project.getClasspath().stream().map(PathEntry::getPath).collect(Collectors.toList())));

        assertEquals(deserializedProject.getModules().size(), project.getModules().size());

        Iterator<Module> modules = deserializedProject.getModules().iterator();
        Iterator<Module> modulesClone = project.getModules().iterator();
        while (modules.hasNext()) {
            Module module = modules.next();
            Module moduleClone = modulesClone.next();

            assertEquals(module.getName(), collapseExtraWhitespaces(moduleClone.getName()));
            assertEquals(module.getRulesRootPath().getPath(),
                    collapseExtraWhitespaces(moduleClone.getRulesRootPath().getPath()));
            assertEquals(module.getMethodFilter().getIncludes().size(), moduleClone.getMethodFilter().getIncludes().size());
            assertEquals(module.getMethodFilter().getExcludes().size(), moduleClone.getMethodFilter().getExcludes().size());

            assertIterableEquals(module.getMethodFilter().getIncludes(), collapseExtraWhitespaces(moduleClone.getMethodFilter().getIncludes()));
            assertIterableEquals(module.getMethodFilter().getExcludes(), collapseExtraWhitespaces(moduleClone.getMethodFilter().getExcludes()));
            assertNull(module.getProject());
            assertNull(module.getProperties());
            assertNull(module.getWildcardName());
            assertNull(module.getWildcardRulesRootPath());
        }

        assertNull(deserializedProject.getOpenapi());
        assertNull(deserializedProject.getDependencies());
        assertNull(deserializedProject.getPropertiesFileNamePatterns());
        assertNull(deserializedProject.getPropertiesFileNameProcessor());
        assertNull(deserializedProject.getProjectFolder());
    }
}

