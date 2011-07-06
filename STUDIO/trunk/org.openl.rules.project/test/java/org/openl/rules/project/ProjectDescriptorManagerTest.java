package org.openl.rules.project;

import static junit.framework.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;

public class ProjectDescriptorManagerTest {

    @Test
    public void testReadDescriptor1() throws FileNotFoundException, ValidationException {

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        ProjectDescriptor descriptor = manager.readDescriptor("test/resources/descriptor/rules1.xml");

        assertEquals("my-project-id", descriptor.getId());
        assertEquals("Project name", descriptor.getName());
        assertEquals("comment", descriptor.getComment());
        assertEquals(2, descriptor.getModules().size());

        Module module1 = descriptor.getModules().get(0);
        assertEquals("MyModule1", module1.getName());
        assertTrue(new File(module1.getRulesRootPath().getPath()).isAbsolute());
        assertEquals(ModuleType.STATIC, module1.getType());
        assertEquals("com.test.MyWrapper", module1.getClassname());

        Module module2 = descriptor.getModules().get(1);
        assertEquals("MyModule2", module2.getName());
        assertTrue(new File(module2.getRulesRootPath().getPath()).isAbsolute());
        assertEquals(ModuleType.API, module2.getType());
        assertNull(module2.getClassname());

        assertEquals(2, descriptor.getClasspath().size());

        PathEntry classpathEntry1 = descriptor.getClasspath().get(0);
        assertEquals("path1", classpathEntry1.getPath());

        PathEntry classpathEntry2 = descriptor.getClasspath().get(1);
        assertEquals("path2", classpathEntry2.getPath());
    }

    @Test(expected = ValidationException.class)
    public void testReadDescriptor2() throws FileNotFoundException, ValidationException {

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        manager.readDescriptor("test/resources/descriptor/rules2.xml");
    }

    @Test(expected = ValidationException.class)
    public void testReadDescriptor3() throws FileNotFoundException, ValidationException {

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        manager.readDescriptor("test/resources/descriptor/rules3.xml");
    }

    @Test
    public void testWriteDescriptor1() throws IOException, ValidationException {

        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setId("id1");
        descriptor.setName("name1");
        descriptor.setComment("comment1");

        Module module1 = new Module();
        module1.setName("name1");
        module1.setRulesRootPath(new PathEntry("path1"));
        module1.setType(ModuleType.STATIC);
        module1.setClassname("MyWrapper1");

        List<PathEntry> classpath = new ArrayList<PathEntry>();
        PathEntry entry1 = new PathEntry("path1");

        PathEntry entry2 = new PathEntry("path2");

        classpath.add(entry1);
        classpath.add(entry2);

        descriptor.setClasspath(classpath);

        List<Module> modules = new ArrayList<Module>();
        modules.add(module1);

        descriptor.setModules(modules);

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        manager.writeDescriptor(descriptor, dest);

        String expected = 
                        "<project>" + "\n" + 
                        "  <id>id1</id>" + "\n" + 
                        "  <name>name1</name>" + "\n" + 
                        "  <comment>comment1</comment>" + "\n" + 
                        "  <modules>" + "\n" + 
                        "    <module>" + "\n" + 
                        "      <name>name1</name>" + "\n" + 
                        "      <type>static</type>" + "\n" + 
                        "      <classname>MyWrapper1</classname>" + "\n" + 
                        "      <rules-root path=\"path1\"/>" + "\n" + 
                        "    </module>" + "\n" + 
                        "  </modules>" + "\n" + 
                        "  <classpath>" + "\n" + 
                        "    <entry path=\"path1\"/>" + "\n" + 
                        "    <entry path=\"path2\"/>" + "\n" + 
                        "  </classpath>" + "\n" + 
                        "</project>";

        assertEquals(expected, dest.toString());
    }
    
    @Test(expected = ValidationException.class)
    public void testWriteDescriptor2() throws IOException, ValidationException {

        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setId("id1");
        descriptor.setName("name1");

        Module module1 = new Module();
        module1.setName("name1");
        module1.setRulesRootPath(new PathEntry("path1"));
        module1.setType(ModuleType.STATIC);
        module1.setClassname("MyWrapper1");

        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        manager.writeDescriptor(descriptor, dest);
    }
    
    @Test(expected = ValidationException.class)
    public void testWriteDescriptor3() throws IOException, ValidationException {

        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setId("id1");
        descriptor.setName("name1");

        Module module1 = new Module();
        module1.setName("name1");
        module1.setRulesRootPath(new PathEntry("path1"));
        module1.setType(ModuleType.STATIC);

        List<PathEntry> classpath = new ArrayList<PathEntry>();
        PathEntry entry1 = new PathEntry("path1");

        PathEntry entry2 = new PathEntry("path2");

        classpath.add(entry1);
        classpath.add(entry2);

        descriptor.setClasspath(classpath);

        List<Module> modules = new ArrayList<Module>();
        modules.add(module1);

        descriptor.setModules(modules);
        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        manager.writeDescriptor(descriptor, dest);
    }
}
