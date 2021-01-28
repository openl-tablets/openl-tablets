package org.openl.rules.project;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

public class ProjectDescriptorManagerTest {

    private static final Path DESCRIPTOR_PATH = Paths.get("test-resources/descriptor.zip");

    @Test
    public void testReadDescriptor1() throws IOException, ValidationException {
        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        ProjectDescriptor descriptor = manager.readDescriptor("test-resources/descriptor/rules1.xml");
        assertReadDescriptor1(descriptor);
        final Path rootFolder = Paths.get("test-resources/descriptor").toAbsolutePath();
        Module module1 = descriptor.getModules().get(0);
        assertTrue(module1.getRulesPath().startsWith(rootFolder));
        Module module2 = descriptor.getModules().get(1);
        assertTrue(module2.getRulesPath().startsWith(rootFolder));
    }

    public void assertReadDescriptor1(ProjectDescriptor descriptor) {
        assertEquals("Project name", descriptor.getName());
        assertEquals("comment", descriptor.getComment());
        assertEquals(2, descriptor.getModules().size());
        assertArrayEquals(new String[] { "%lob%" }, descriptor.getPropertiesFileNamePatterns());
        assertEquals("default.DefaultPropertiesFileNameProcessor", descriptor.getPropertiesFileNameProcessor());
        Module module1 = descriptor.getModules().get(0);
        assertEquals("MyModule1", module1.getName());
        assertEquals("MyModule1.xls",
            module1.getRulesPath().getName(module1.getRulesPath().getNameCount() - 1).toString());
        assertEquals("MyModule1.xls", module1.getRulesRootPath().getPath());
        assertTrue(module1.getRulesPath().isAbsolute());

        Module module2 = descriptor.getModules().get(1);
        assertEquals("MyModule2", module2.getName());
        assertEquals("MyModule2.xls",
            module2.getRulesPath().getName(module2.getRulesPath().getNameCount() - 1).toString());
        assertEquals("MyModule2.xls", module2.getRulesRootPath().getPath());
        assertTrue(module2.getRulesPath().isAbsolute());

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

    @Test
    public void zipArchive_testReadDescriptor1() throws IOException, ValidationException {
        try (FileSystem fs = openZipFile(DESCRIPTOR_PATH)) {
            final Path rootFolder = fs.getPath("/");
            ProjectDescriptor descriptor = new ProjectDescriptorManager().readDescriptor(fs.getPath("/rules1.xml"));
            assertReadDescriptor1(descriptor);
            Module module1 = descriptor.getModules().get(0);
            assertTrue(module1.getRulesPath().startsWith(rootFolder));
            Module module2 = descriptor.getModules().get(1);
            assertTrue(module2.getRulesPath().startsWith(rootFolder));
        }
    }

    @Test(expected = ValidationException.class)
    public void testReadDescriptor2() throws IOException, ValidationException {
        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        manager.readDescriptor("test-resources/descriptor/rules2.xml");
    }

    @Test(expected = ValidationException.class)
    public void zipArchive_testReadDescriptor2() throws IOException, ValidationException {
        try (FileSystem fs = openZipFile(DESCRIPTOR_PATH)) {
            new ProjectDescriptorManager().readDescriptor(fs.getPath("/rules2.xml"));
        }
    }

    @Test(expected = ValidationException.class)
    public void testReadDescriptor3() throws IOException, ValidationException {
        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        manager.readDescriptor("test-resources/descriptor/rules3.xml");
    }

    @Test(expected = ValidationException.class)
    public void zipArchive_testReadDescriptor3() throws IOException, ValidationException {
        try (FileSystem fs = openZipFile(DESCRIPTOR_PATH)) {
            new ProjectDescriptorManager().readDescriptor(fs.getPath("/rules3.xml"));
        }
    }

    @Test
    public void testIsCoveredByWildcardModule() throws IOException {
        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
        final ProjectDescriptor descriptor = serializer
            .deserialize(new FileInputStream("test-resources/descriptor/rules-wildcard.xml"));
        assertIsCoveredByWildcardModule(manager, descriptor);
    }

    private void assertIsCoveredByWildcardModule(ProjectDescriptorManager manager, ProjectDescriptor descriptor) {
        Module newModule = new Module();
        newModule.setName("New Module");
        newModule.setRulesRootPath(new PathEntry("rules/New Module.xlsx"));
        assertTrue(manager.isCoveredByWildcardModule(descriptor, newModule));

        newModule.setRulesRootPath(new PathEntry("rules\\New Module.xlsx"));
        assertTrue(manager.isCoveredByWildcardModule(descriptor, newModule));

        newModule.setRulesRootPath(new PathEntry("New Module.xlsx"));
        assertFalse(manager.isCoveredByWildcardModule(descriptor, newModule));
    }

    @Test
    public void zipArchive_testIsCoveredByWildcardModule() throws IOException {
        try (FileSystem fs = openZipFile(DESCRIPTOR_PATH)) {
            ProjectDescriptorManager manager = new ProjectDescriptorManager();
            XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
            final ProjectDescriptor descriptor = serializer
                .deserialize(Files.newInputStream(fs.getPath("/rules-wildcard.xml")));
            assertIsCoveredByWildcardModule(manager, descriptor);
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteDescriptor1() throws IOException, ValidationException {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setId("id1"); // As far as id was deprecated, it should not be saved to xml.
        descriptor.setName("name1");
        descriptor.setComment("comment1");
        descriptor.setPropertiesFileNamePatterns(new String[] { "{lob}" });
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
    public void zipArchive_testModulePathPatterns() throws Exception {
        try (FileSystem fs = openZipFile(Paths.get("test-resources/test-resources.zip"))) {
            ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();
            // test ?
            assertEquals(6, projectDescriptorManager.readDescriptor(fs.getPath("/rules1.xml")).getModules().size());
            // test *
            assertEquals(2, projectDescriptorManager.readDescriptor(fs.getPath("/rules2.xml")).getModules().size());
            // test **
            assertEquals(2, projectDescriptorManager.readDescriptor(fs.getPath("/rules3.xml")).getModules().size());
            // test complex
            assertEquals(2, projectDescriptorManager.readDescriptor(fs.getPath("/rules4.xml")).getModules().size());
        }
    }

    @Test
    public void testClassPathUrls() throws Exception {
        ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();
        ProjectDescriptor projectDescriptor = projectDescriptorManager
            .readDescriptor("./test-resources/descriptor/rules-clspth.xml");
        URL[] classPathUrls = projectDescriptor.getClassPathUrls();
        assertEquals(10, classPathUrls.length);
    }

    @Test
    public void zipArchive_testClassPathUrls() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_PATH)) {
            ProjectDescriptor projectDescriptor = new ProjectDescriptorManager()
                .readDescriptor(fs.getPath("/rules-clspth.xml"));
            assertEquals(10, projectDescriptor.getClassPathUrls().length);
            assertArrayEquals(projectDescriptor.getClassPathUrls(), projectDescriptor.getClassPathUrls());
        }
    }

    @Test
    public void zipArchive_testClassPathUrls_Internal() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_PATH)) {
            ProjectDescriptor projectDescriptor = new ProjectDescriptorManager()
                .readDescriptor(fs.getPath("/internal/rules-clspth.xml"));
            assertEquals(10, projectDescriptor.getClassPathUrls().length);
            assertArrayEquals(projectDescriptor.getClassPathUrls(), projectDescriptor.getClassPathUrls());
        }
    }

    private static FileSystem openZipFile(Path path) throws IOException {
        return FileSystems.newFileSystem(path, Thread.currentThread().getContextClassLoader());
    }
}
