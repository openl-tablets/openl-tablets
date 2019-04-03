package org.openl.rules.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;

public class ProjectDescriptorManagerTest {

    @Test
    public void testReadDescriptor1() throws IOException, ValidationException {

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        ProjectDescriptor descriptor = manager.readDescriptor("test-resources/descriptor/rules1.xml");

        assertEquals("Project name", descriptor.getName());
        assertEquals("comment", descriptor.getComment());
        assertEquals(2, descriptor.getModules().size());
        assertEquals("%lob%", descriptor.getPropertiesFileNamePattern());
        assertEquals("default.DefaultPropertiesFileNameProcessor", descriptor.getPropertiesFileNameProcessor());
        Module module1 = descriptor.getModules().get(0);
        assertEquals("MyModule1", module1.getName());
        assertTrue(new File(module1.getRulesRootPath().getPath()).isAbsolute());

        Module module2 = descriptor.getModules().get(1);
        assertEquals("MyModule2", module2.getName());
        assertTrue(new File(module2.getRulesRootPath().getPath()).isAbsolute());

        assertEquals(2, descriptor.getClasspath().size());

        PathEntry classpathEntry1 = descriptor.getClasspath().get(0);
        assertEquals("path1", classpathEntry1.getPath());

        PathEntry classpathEntry2 = descriptor.getClasspath().get(1);
        assertEquals("path2", classpathEntry2.getPath());

        assertNotNull(descriptor.getModules());
        assertEquals(2, descriptor.getModules().size());
        Module module = descriptor.getModules().get(0);
        if (!"MyModule2".equals(module.getName())) {
            module = descriptor.getModules().get(1);
        }
        assertNotNull(module.getMethodFilter());
        assertNotNull(module.getMethodFilter().getIncludes());
        assertEquals(1, module.getMethodFilter().getIncludes().size());
        assertNotNull(module.getMethodFilter().getExcludes());
        Iterator<String> itr = module.getMethodFilter().getIncludes().iterator();
        String value = itr.next();
        assertEquals("*", value);

        assertNotNull(descriptor.getDependencies());
        assertEquals(1, descriptor.getDependencies().size());
        ProjectDependencyDescriptor projectDependencyDescriptor = descriptor.getDependencies().iterator().next();
        assertEquals("someProjectName", projectDependencyDescriptor.getName());
        assertFalse(projectDependencyDescriptor.isAutoIncluded());
    }

    @Test(expected = ValidationException.class)
    public void testReadDescriptor2() throws IOException, ValidationException {

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        manager.readDescriptor("test-resources/descriptor/rules2.xml");
    }

    @Test(expected = ValidationException.class)
    public void testReadDescriptor3() throws IOException, ValidationException {

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        manager.readDescriptor("test-resources/descriptor/rules3.xml");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteDescriptor1() throws IOException, ValidationException {

        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setId("id1"); // As far as id was deprecated, it should not be saved to xml.
        descriptor.setName("name1");
        descriptor.setComment("comment1");
        descriptor.setPropertiesFileNamePattern("{lob}");
        descriptor.setPropertiesFileNameProcessor("default.DefaultPropertiesFileNameProcessor");

        List<ProjectDependencyDescriptor> dependencies = new ArrayList<>();
        ProjectDependencyDescriptor dependencyDescriptor = new ProjectDependencyDescriptor();
        dependencyDescriptor.setName("someProjectName");
        dependencyDescriptor.setAutoIncluded(false);
        dependencies.add(dependencyDescriptor);
        descriptor.setDependencies(dependencies);

        Module module1 = new Module();
        module1.setName("name1");
        module1.setRulesRootPath(new PathEntry("path1"));
        module1.setMethodFilter(new MethodFilter());

        List<PathEntry> classpath = new ArrayList<>();
        PathEntry entry1 = new PathEntry("path1");

        PathEntry entry2 = new PathEntry("path2");

        classpath.add(entry1);
        classpath.add(entry2);

        descriptor.setClasspath(classpath);

        List<Module> modules = new ArrayList<>();
        modules.add(module1);

        descriptor.setModules(modules);

        module1.getMethodFilter().addIncludePattern(" * ");

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        manager.writeDescriptor(descriptor, dest);

        String expected = "<project>" + "\n" + "  <name>name1</name>" + "\n" + "  <comment>comment1</comment>" + "\n" + "  <modules>" + "\n" + "    <module>" + "\n" + "      <name>name1</name>" + "\n" + "      <rules-root path=\"path1\"/>" + "\n" + "      <method-filter>" + "\n" + "        <includes>" + "\n" + "          <value>*</value>" + "\n" + "        </includes>" + "\n" + "      </method-filter>" + "\n" + "    </module>" + "\n" + "  </modules>" + "\n" + "  <classpath>" + "\n" + "    <entry path=\"path1\"/>" + "\n" + "    <entry path=\"path2\"/>" + "\n" + "  </classpath>" + "\n" + "  <dependencies>" + "\n" + "    <dependency>" + "\n" + "      <name>someProjectName</name>" + "\n" + "      <autoIncluded>false</autoIncluded>" + "\n" + "    </dependency>" + "\n" + "  </dependencies>" + "\n" + "  <properties-file-name-pattern>{lob}</properties-file-name-pattern>" + "\n" + "  <properties-file-name-processor>default.DefaultPropertiesFileNameProcessor</properties-file-name-processor>" + "\n" + "</project>";
        assertEquals(expected, dest.toString());
    }

    @Test(expected = ValidationException.class)
    public void testWriteDescriptor2() throws IOException, ValidationException {

        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("name1");

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        manager.writeDescriptor(descriptor, dest);
    }

    @Test
    public void testModulePathPatterns() throws Exception {
        ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();
        // test ?
        assertEquals(6, projectDescriptorManager.readDescriptor("./test-resources/rules1.xml").getModules().size());
        // test *
        assertEquals(2, projectDescriptorManager.readDescriptor("./test-resources/rules2.xml").getModules().size());
        // test **
        assertEquals(2, projectDescriptorManager.readDescriptor("./test-resources/rules3.xml").getModules().size());
        // test complex
        assertEquals(2, projectDescriptorManager.readDescriptor("./test-resources/rules4.xml").getModules().size());
    }

    @Test
    public void testClassPathUrls() throws Exception {
        ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();
        ProjectDescriptor projectDescriptor = projectDescriptorManager
            .readDescriptor("./test-resources/descriptor/rules-clspth.xml");
        URL[] classPathUrls = projectDescriptor.getClassPathUrls();
        assertEquals(9, classPathUrls.length);

    }
}
