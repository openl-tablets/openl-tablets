package org.openl.rules.project.xml.v5_12;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.project.xml.BaseProjectDescriptorSerializerTest.collapseExtraWhitespaces;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_12.ProjectDescriptor_v5_12;
import org.openl.rules.project.model.v5_12.converter.ProjectDescriptorVersionConverter_v5_12;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializerTest;

public class XmlProjectDescriptorSerializer_v5_12Test {

    @Test
    public void testSerialize() throws Exception {
        ProjectDescriptor projectDescriptor = BaseProjectDescriptorSerializerTest.initProjectDescriptorForTest();

        String xml = new BaseProjectDescriptorSerializer<>(
                new ProjectDescriptorVersionConverter_v5_12(), ProjectDescriptor_v5_12.class).serialize(projectDescriptor);
        assertEquals(xml, PROJECT_DESCRIPTOR_V5_12_XML);
    }

    public static final String PROJECT_DESCRIPTOR_V5_12_XML = "<project>\n" +
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
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <name>ProjectDependencyDescriptor Name</name>\n" +
            "            <autoIncluded>true</autoIncluded>\n" +
            "        </dependency>\n" +
            "    </dependencies>\n" +
            "    <properties-file-name-pattern>{lob}</properties-file-name-pattern>\n" +
            "    <properties-file-name-processor>default.DefaultProcessor</properties-file-name-processor>\n" +
            "</project>";

    @Test
    public void testDeserialize() throws Exception {
        ProjectDescriptor deserializedProject = new BaseProjectDescriptorSerializer<>(
                new ProjectDescriptorVersionConverter_v5_12(), ProjectDescriptor_v5_12.class).deserialize(
                new FileInputStream("test-resources/org.openl.rules.project.xml/project-descriptor_v5_12.xml"));
        assertProjectDescriptorDeserializedCorrectly(deserializedProject);
    }

    // same for v5_12, v5_13, v5_16
    public static void assertProjectDescriptorDeserializedCorrectly(ProjectDescriptor deserializedProject) {
        ProjectDescriptor project = BaseProjectDescriptorSerializerTest.initProjectDescriptorForTest();

        assertNull(deserializedProject.getId());
        assertEquals(deserializedProject.getName(), collapseExtraWhitespaces(project.getName()));
        assertEquals(deserializedProject.getComment(), collapseExtraWhitespaces(project.getComment()));
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

            assertEquals(module.getMethodFilter().getIncludes(), collapseExtraWhitespaces(moduleClone.getMethodFilter().getIncludes()));
            assertEquals(module.getMethodFilter().getExcludes(), collapseExtraWhitespaces(moduleClone.getMethodFilter().getExcludes()));
            assertNull(module.getProject());
            assertNull(module.getProperties());
            assertNull(module.getWildcardName());
            assertNull(module.getWildcardRulesRootPath());
        }

        assertNull(deserializedProject.getOpenapi());
        assertEquals(deserializedProject.getDependencies().size(), project.getDependencies().size());
        Iterator<ProjectDependencyDescriptor> deserializedPDDIterator = deserializedProject.getDependencies().iterator();
        Iterator<ProjectDependencyDescriptor> pDDIterator = project.getDependencies().iterator();
        while (deserializedPDDIterator.hasNext()) {
            ProjectDependencyDescriptor deserializedPDD = deserializedPDDIterator.next();
            ProjectDependencyDescriptor pDD = pDDIterator.next();
            assertEquals(deserializedPDD.getName(), collapseExtraWhitespaces(pDD.getName()));
            assertEquals(deserializedPDD.isAutoIncluded(), pDD.isAutoIncluded());
        }

        assertEquals(1, deserializedProject.getPropertiesFileNamePatterns().length);
        assertEquals(deserializedProject.getPropertiesFileNamePatterns()[0],
                collapseExtraWhitespaces(project.getPropertiesFileNamePatterns()[0]));
        assertEquals(deserializedProject.getPropertiesFileNameProcessor(),
                collapseExtraWhitespaces(project.getPropertiesFileNameProcessor()));
        assertNull(deserializedProject.getProjectFolder());
    }
}

