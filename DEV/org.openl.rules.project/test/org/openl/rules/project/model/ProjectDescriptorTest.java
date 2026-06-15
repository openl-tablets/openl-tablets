package org.openl.rules.project.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ProjectDescriptorTest {

    private static final Path DESCRIPTOR_ZIP = Path.of("test-resources/descriptor.zip");

    @Test
    void testRelativeUri() {
        ProjectDescriptor pd = new ProjectDescriptor();
        pd.setProjectFolder(Path.of("test/rules/test xls"));
        assertEquals("test%20xls", pd.getRelativeUri());
    }

    @Test
    void equalsAndHashCodeAreNullNameSafe() {
        // 'name' is nullable until expand() fills it - equals()/hashCode() must not throw on a null name
        ProjectDescriptor nullName1 = new ProjectDescriptor();
        ProjectDescriptor nullName2 = new ProjectDescriptor();
        ProjectDescriptor named = new ProjectDescriptor();
        named.setName("project");

        assertEquals(nullName1, nullName1);
        assertEquals(nullName1, nullName2);
        assertEquals(nullName1.hashCode(), nullName2.hashCode());

        assertNotEquals(nullName1, named);
        assertNotEquals(named, nullName1);
        assertNotEquals(nullName1, "project");

        // a null-name descriptor must survive hash-based collections (relies on both equals() and hashCode())
        Set<ProjectDescriptor> descriptors = new HashSet<>();
        descriptors.add(nullName1);
        assertTrue(descriptors.contains(nullName2));
        assertFalse(descriptors.contains(named));
    }

    @Test
    void testReadDescriptor1() throws Exception {
        var descriptor = ProjectDescriptor.read(Path.of("test-resources/descriptor/rules1.xml")).expand();
        assertReadDescriptor1(descriptor);
        final Path rootFolder = Path.of("test-resources/descriptor").toAbsolutePath();
        Module module1 = descriptor.getModules().getFirst();
        assertTrue(module1.getRulesPath().startsWith(rootFolder));
        Module module2 = descriptor.getModules().get(1);
        assertTrue(module2.getRulesPath().startsWith(rootFolder));
    }

    @Test
    void zipArchive_testReadDescriptor1() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            final Path rootFolder = fs.getPath("/");
            var descriptor = ProjectDescriptor.read(fs.getPath("/rules1.xml")).expand();
            assertReadDescriptor1(descriptor);
            Module module1 = descriptor.getModules().getFirst();
            assertTrue(module1.getRulesPath().startsWith(rootFolder));
            Module module2 = descriptor.getModules().get(1);
            assertTrue(module2.getRulesPath().startsWith(rootFolder));
        }
    }

    private void assertReadDescriptor1(ProjectDescriptor descriptor) {
        assertEquals("Project name", descriptor.getName());
        assertEquals("comment", descriptor.getComment());
        assertEquals(2, descriptor.getModules().size());
        assertArrayEquals(new String[]{"%lob%"}, descriptor.getPropertiesFileNamePatterns());
        assertEquals("default.DefaultPropertiesFileNameProcessor", descriptor.getPropertiesFileNameProcessor());
        Module module1 = descriptor.getModules().getFirst();
        assertEquals("MyModule1", module1.getName());
        assertEquals("MyModule1.xls",
                module1.getRulesPath().getName(module1.getRulesPath().getNameCount() - 1).toString());
        assertEquals("MyModule1.xls", module1.getRulesRootPath());
        assertTrue(module1.getRulesPath().isAbsolute());

        Module module2 = descriptor.getModules().get(1);
        assertEquals("MyModule2", module2.getName());
        assertEquals("MyModule2.xls",
                module2.getRulesPath().getName(module2.getRulesPath().getNameCount() - 1).toString());
        assertEquals("MyModule2.xls", module2.getRulesRootPath());
        assertTrue(module2.getRulesPath().isAbsolute());

        assertEquals(2, descriptor.getClasspath().size());

        String classpathEntry1 = descriptor.getClasspath().getFirst();
        assertEquals("path1", classpathEntry1);

        String classpathEntry2 = descriptor.getClasspath().get(1);
        assertEquals("path2", classpathEntry2);

        assertNotNull(descriptor.getModules());
        assertEquals(2, descriptor.getModules().size());
        Module module = descriptor.getModules().getFirst();
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
        ProjectDependencyDescriptor projectDependencyDescriptor = descriptor.getDependencies().getFirst();
        assertEquals("someProjectName", projectDependencyDescriptor.getName());
        assertFalse(projectDependencyDescriptor.isAutoIncluded());
    }

    @Test
    void testReadDescriptor2() throws Exception {
        var pd = ProjectDescriptor.read(Path.of("test-resources/descriptor/rules2.xml")).expand();
        assertEquals(2, pd.getModules().size());
    }

    @Test
    void zipArchive_testReadDescriptor2() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            var pd = ProjectDescriptor.read(fs.getPath("/rules2.xml")).expand();
            assertEquals(2, pd.getModules().size());
        }
    }

    @Test
    void testReadDescriptor3_emptyModules() throws Exception {
        var descriptor = ProjectDescriptor.read(Path.of("test-resources/descriptor/rules3.xml"));
        assertTrue(descriptor.getModules().isEmpty());
    }

    @Test
    void zipArchive_testReadDescriptor3_emptyModules() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            var descriptor = ProjectDescriptor.read(fs.getPath("/rules3.xml")).expand();
            assertTrue(descriptor.getModules().isEmpty());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    void testWriteDescriptor1() throws Exception {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("name1");
        descriptor.setComment("comment1");
        descriptor.setPropertiesFileNamePatterns(new String[]{"{lob}"});
        descriptor.setPropertiesFileNameProcessor("default.DefaultPropertiesFileNameProcessor");

        List<ProjectDependencyDescriptor> dependencies = new ArrayList<>();
        ProjectDependencyDescriptor dependencyDescriptor = new ProjectDependencyDescriptor();
        dependencyDescriptor.setName("someProjectName");
        dependencyDescriptor.setAutoIncluded(false);
        dependencies.add(dependencyDescriptor);
        descriptor.setDependencies(dependencies);
        descriptor.setExposedMethods(new ExposedMethods());
        descriptor.getExposedMethods().setIncludes(Set.of(" INCL "));
        descriptor.getExposedMethods().setExcludes(Set.of(" excl "));

        Module module1 = new Module();
        module1.setName("name1");
        module1.setRulesRootPath("path1");
        module1.setMethodFilter(new MethodFilter());

        List<String> classpath = new ArrayList<>();
        classpath.add("path1");
        classpath.add("path2");
        descriptor.setClasspath(classpath);

        List<Module> modules = new ArrayList<>();
        modules.add(module1);
        descriptor.setModules(modules);

        module1.getMethodFilter().addIncludePattern(" * ");
        module1.getMethodFilter().addExcludePattern(" * ");
        module1.getMethodFilter().addExcludePattern("  ");

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        String expected = """
                <project>
                    <name>name1</name>
                    <comment>comment1</comment>
                    <modules>
                        <module>
                            <name>name1</name>
                            <rules-root path="path1"/>
                            <method-filter>
                                <includes>
                                    <value>*</value>
                                </includes>
                                <excludes>
                                    <value>*</value>
                                </excludes>
                            </method-filter>
                        </module>
                    </modules>
                    <classpath>
                        <entry path="path1"/>
                        <entry path="path2"/>
                    </classpath>
                    <dependencies>
                        <dependency>
                            <name>someProjectName</name>
                        </dependency>
                    </dependencies>
                    <properties-file-name-pattern>{lob}</properties-file-name-pattern>
                    <properties-file-name-processor>default.DefaultPropertiesFileNameProcessor</properties-file-name-processor>
                    <exposed-methods>
                        <include>INCL</include>
                        <exclude>excl</exclude>
                    </exposed-methods>
                </project>
                """;
        assertEquals(expected, dest);
    }

    @Test
    void testModulePathPatterns() throws Exception {
        // test ?
        assertEquals(4, ProjectDescriptor.read(Path.of("./test-resources/rules1.xml")).expand().getModules().size());
        // test *
        assertEquals(2, ProjectDescriptor.read(Path.of("./test-resources/rules2.xml")).expand().getModules().size());
        // test **
        assertEquals(2, ProjectDescriptor.read(Path.of("./test-resources/rules3.xml")).expand().getModules().size());
        // test complex
        assertEquals(2, ProjectDescriptor.read(Path.of("./test-resources/rules4.xml")).expand().getModules().size());
    }

    @Test
    void zipArchive_testModulePathPatterns() throws Exception {
        try (FileSystem fs = openZipFile(Path.of("test-resources/test-resources.zip"))) {
            // test ?
            assertEquals(6, ProjectDescriptor.read(fs.getPath("/rules1.xml")).expand().getModules().size());
            // test *
            assertEquals(2, ProjectDescriptor.read(fs.getPath("/rules2.xml")).expand().getModules().size());
            // test **
            assertEquals(2, ProjectDescriptor.read(fs.getPath("/rules3.xml")).expand().getModules().size());
            // test complex
            assertEquals(2, ProjectDescriptor.read(fs.getPath("/rules4.xml")).expand().getModules().size());
        }
    }

    @Test
    void testClassPathUrls() throws Exception {
        var projectDescriptor = ProjectDescriptor.read(Path.of("./test-resources/descriptor/rules-clspth.xml"));
        assertEquals(10, projectDescriptor.getClassPathUrls().length);
    }

    @Test
    void zipArchive_testClassPathUrls() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            var projectDescriptor = ProjectDescriptor.read(fs.getPath("/rules-clspth.xml")).expand();
            assertEquals(10, projectDescriptor.getClassPathUrls().length);
            assertArrayEquals(projectDescriptor.getClassPathUrls(), projectDescriptor.getClassPathUrls());
        }
    }

    @Test
    void zipArchive_testClassPathUrls_internalDescriptor() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            var projectDescriptor = ProjectDescriptor.read(fs.getPath("/internal/rules-clspth.xml")).expand();
            assertEquals(10, projectDescriptor.getClassPathUrls().length);
            assertArrayEquals(projectDescriptor.getClassPathUrls(), projectDescriptor.getClassPathUrls());
        }
    }

    @Test
    void zipArchive_releaseClassPathDeletesExtractedTempJars() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            var projectDescriptor = ProjectDescriptor.read(fs.getPath("/rules-clspth.xml")).expand();

            // Nested jars inside the project archive are extracted to temp files in java.io.tmpdir.
            List<Path> tempJars = new ArrayList<>();
            for (URL url : projectDescriptor.getClassPathUrls()) {
                Path path = toFilePath(url);
                if (path != null && path.getFileName().toString().startsWith("tmp-")) {
                    tempJars.add(path);
                }
            }
            assertFalse(tempJars.isEmpty(), "expected nested jars to be extracted to temp files");
            for (Path tempJar : tempJars) {
                assertTrue(Files.exists(tempJar), "temp jar must exist before release: " + tempJar);
            }

            projectDescriptor.releaseClassPath();

            for (Path tempJar : tempJars) {
                assertFalse(Files.exists(tempJar), "temp jar must be deleted after release: " + tempJar);
            }
        }
    }

    @Test
    void zipArchive_extractedTempJarsAreDeletedWhenDescriptorIsCollected() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            List<Path> tempJars = extractClasspathTempJars(fs);
            assertFalse(tempJars.isEmpty(), "expected nested jars to be extracted to temp files");
            tempJars.forEach(p -> assertTrue(Files.exists(p), "temp jar must exist before GC: " + p));

            // No explicit releaseClassPath(): the descriptor is dropped, like callers that never release it
            // (PropertiesFileNameProcessorBuilder / ProjectResourceLoader). The Cleaner must delete the temp jars
            // once the descriptor is garbage-collected.
            awaitDeleted(tempJars);
        }
    }

    @Test
    void testReadPropertiesFileNamePattern() throws Exception {
        ProjectDescriptor pd = ProjectDescriptor.read(Path.of("test-resources/xml/rules1.xml"));

        assertArrayEquals(new String[]{"properties-file-name-pattern"}, pd.getPropertiesFileNamePatterns());
        assertEquals("properties-file-name-processor", pd.getPropertiesFileNameProcessor());
    }

    @Test
    void testReadMultiPropertiesFileNamePatterns() throws Exception {
        ProjectDescriptor pd = ProjectDescriptor.read(Path.of("test-resources/multi-file-name-pattern/rules.xml"));

        assertEquals("test ?", pd.getName());
        List<Module> modules = pd.getModules();
        assertNotNull(modules);
        assertEquals(1, modules.size());
        Module m = modules.getFirst();
        assertEquals("testmodule", m.getName());
        assertEquals("dependencies/test3/module/dependency-module?/dependency?.xls", m.getRulesRootPath());
        assertArrayEquals(new String[]{"%lob%-%usState%", "Tests-*", "DataTables"}, pd.getPropertiesFileNamePatterns());
    }

    @Test
    void testReadExposedMethods() throws Exception {
        ProjectDescriptor pd = ProjectDescriptor.read(Path.of("test-resources/xml/rules-with-exposed-methods.xml"));

        assertNotNull(pd.getExposedMethods());
        assertNotNull(pd.getExposedMethods().getIncludes());
        assertEquals(2, pd.getExposedMethods().getIncludes().size());
        assertTrue(pd.getExposedMethods().getIncludes().contains("get*"));
        assertTrue(pd.getExposedMethods().getIncludes().contains("calculatePremium"));
        assertNotNull(pd.getExposedMethods().getExcludes());
        assertEquals(1, pd.getExposedMethods().getExcludes().size());
        assertTrue(pd.getExposedMethods().getExcludes().contains("internal*"));
    }

    @Test
    void testReadWithoutExposedMethods() throws Exception {
        ProjectDescriptor pd = ProjectDescriptor.read(Path.of("test-resources/xml/rules1.xml"));

        assertNull(pd.getExposedMethods());
    }

    @Test
    void testExposedMethodsRoundTrip() throws Exception {
        var pd = ProjectDescriptor.read(Path.of("test-resources/xml/rules-with-exposed-methods.xml"));

        var pd2 = ProjectDescriptor.read(new ByteArrayInputStream(pd.toBytes()));

        assertNotNull(pd2.getExposedMethods());
        assertEquals(pd.getExposedMethods().getIncludes().size(), pd2.getExposedMethods().getIncludes().size());
        assertTrue(pd2.getExposedMethods().getIncludes().contains("get*"));
        assertTrue(pd2.getExposedMethods().getIncludes().contains("calculatePremium"));
        assertEquals(pd.getExposedMethods().getExcludes().size(), pd2.getExposedMethods().getExcludes().size());
        assertTrue(pd2.getExposedMethods().getExcludes().contains("internal*"));
    }

    @Test
    void testMultiPropertiesFileNamePatternsRoundTrip() throws Exception {
        var pd = ProjectDescriptor.read(Path.of("test-resources/multi-file-name-pattern/rules.xml"));

        var pd1 = ProjectDescriptor.read(new ByteArrayInputStream(pd.toBytes()));

        assertEquals("test ?", pd1.getName());
        List<Module> modules = pd1.getModules();
        assertNotNull(modules);
        assertEquals(1, modules.size());
        Module m = modules.getFirst();
        assertEquals("testmodule", m.getName());
        assertEquals("dependencies/test3/module/dependency-module?/dependency?.xls", m.getRulesRootPath());
        assertArrayEquals(new String[]{"%lob%-%usState%", "Tests-*", "DataTables"}, pd1.getPropertiesFileNamePatterns());
    }

    @Test
    void testWriteOmitsBlankProjectFields() throws Exception {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("   ");
        descriptor.setComment("");
        descriptor.setPropertiesFileNameProcessor("\t");
        descriptor.setPropertiesFileNamePatterns(new String[]{null, "", "  "});
        descriptor.setClasspath(new ArrayList<>(List.of("", "  ")));
        descriptor.setDependencies(new ArrayList<>());
        descriptor.setOpenapi(new OpenAPI("  ", null, "", null));
        ExposedMethods em = new ExposedMethods();
        em.setIncludes(new HashSet<>(Arrays.asList("", null, " ")));
        em.setExcludes(new HashSet<>(Arrays.asList("", null, " ")));
        descriptor.setExposedMethods(em);

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("<project/>\n", dest);
    }

    @Test
    void testWriteFiltersBlankPropertiesFileNamePatterns() throws Exception {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("p");
        descriptor.setPropertiesFileNamePatterns(new String[]{"", "{lob}-{state}", null, "  "});

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>p</name>
                    <properties-file-name-pattern>{lob}-{state}</properties-file-name-pattern>
                </project>
                """, dest);
    }

    @Test
    void testWriteFiltersBlankClasspathEntries() throws Exception {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("p");
        descriptor.setClasspath(new ArrayList<>(List.of("lib/*.jar", "")));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>p</name>
                    <classpath>
                        <entry path="lib/*.jar"/>
                    </classpath>
                </project>
                """, dest);
    }

    @Test
    void testWriteKeepsOpenApiWithPath() throws Exception {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("p");
        descriptor.setOpenapi(new OpenAPI("api.yaml", OpenAPI.Mode.RECONCILIATION, "", null));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>p</name>
                    <openapi>
                        <path>api.yaml</path>
                        <mode>RECONCILIATION</mode>
                    </openapi>
                </project>
                """, dest);
    }

    @Test
    void testWriteOmitsDefaultReconciliationOpenApi() throws Exception {
        for (String defaultPath : List.of("openapi.yaml", "openapi.yml", "openapi.json")) {
            ProjectDescriptor descriptor = new ProjectDescriptor();
            descriptor.setName("p");
            descriptor.setOpenapi(new OpenAPI(defaultPath, OpenAPI.Mode.RECONCILIATION, null, null));

            var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

            assertEquals("""
                    <project>
                        <name>p</name>
                    </project>
                    """, dest, "for path " + defaultPath);
        }
    }

    @Test
    void testWriteKeepsGenerationOpenApiEvenForDefaultPath() throws Exception {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("p");
        descriptor.setOpenapi(new OpenAPI("openapi.yaml", OpenAPI.Mode.GENERATION, null, null));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>p</name>
                    <openapi>
                        <path>openapi.yaml</path>
                        <mode>GENERATION</mode>
                    </openapi>
                </project>
                """, dest);
    }

    @Test
    void testWriteKeepsReconciliationOpenApiWithModelOverrides() throws Exception {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("p");
        descriptor.setOpenapi(new OpenAPI("openapi2.yaml", OpenAPI.Mode.RECONCILIATION, "Model", null));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>p</name>
                    <openapi>
                        <path>openapi2.yaml</path>
                        <model-module-name>Model</model-module-name>
                        <mode>RECONCILIATION</mode>
                    </openapi>
                </project>
                """, dest);
    }

    @Test
    void testWriteDropsModulesWithoutRulesRootPath() throws Exception {
        Module noPath = new Module();
        noPath.setName("orphan");
        Module blankPath = new Module();
        blankPath.setRulesRootPath("  ");
        Module valid = new Module();
        valid.setName("kept");
        valid.setRulesRootPath("rules/A.xlsx");
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("p");
        descriptor.setModules(new ArrayList<>(List.of(noPath, blankPath, valid)));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>p</name>
                    <modules>
                        <module>
                            <name>kept</name>
                            <rules-root path="rules/A.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, dest);
    }

    @Test
    void testReadSkipsBlankAndMissingClasspathEntries() throws Exception {
        var xml = """
                <project>
                    <name>p</name>
                    <classpath>
                        <entry/>
                        <entry path=""/>
                        <entry path="   "/>
                        <entry path="lib/*.jar"/>
                        <entry path="  lib/extra.jar  "/>
                    </classpath>
                </project>
                """;
        var pd = ProjectDescriptor.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(pd);
        assertEquals(2, pd.getClasspath().size());
        assertEquals("lib/*.jar", pd.getClasspath().get(0));
        assertEquals("lib/extra.jar", pd.getClasspath().get(1));
    }

    @Test
    void testReadBlankClasspathEntriesDoesNotBreakClassPathUrlsResolution() throws Exception {
        var xml = """
                <project>
                    <name>p</name>
                    <classpath>
                        <entry/>
                        <entry path="   "/>
                    </classpath>
                </project>
                """;
        var pd = ProjectDescriptor.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        pd.setProjectFolder(Path.of("."));
        // Should not throw NPE inside processClasspathPathPatterns
        assertNotNull(pd.getClassPathUrls());
    }

    @Test
    void testReadTrimsRulesRootPathAttribute() throws Exception {
        var xml = """
                <project>
                    <name>p</name>
                    <modules>
                        <module>
                            <name>m1</name>
                            <rules-root path="  rules/A.xlsx  "/>
                        </module>
                        <module>
                            <name>m-blank</name>
                            <rules-root path="   "/>
                        </module>
                        <module>
                            <name>m-empty</name>
                            <rules-root path=""/>
                        </module>
                    </modules>
                </project>
                """;
        var pd = ProjectDescriptor.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(pd);
        assertEquals(3, pd.getModules().size());
        assertEquals("rules/A.xlsx", pd.getModules().get(0).getRulesRootPath());
        assertNull(pd.getModules().get(1).getRulesRootPath());
        assertNull(pd.getModules().get(2).getRulesRootPath());
    }

    @Test
    void testWriteOmitsEmptyMethodFilterOnValidModule() throws Exception {
        Module module = new Module();
        module.setName("  ");
        module.setRulesRootPath("rules/A.xlsx");
        MethodFilter mf = new MethodFilter();
        mf.setIncludes(new HashSet<>());
        mf.setExcludes(new HashSet<>());
        module.setMethodFilter(mf);
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("p");
        descriptor.setModules(new ArrayList<>(List.of(module)));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>p</name>
                    <modules>
                        <module>
                            <rules-root path="rules/A.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, dest);
    }

    /**
     * Default state of {@link ProjectDependencyDescriptor}: only {@code mavenArtifact} is set; {@code name}
     * and {@code autoIncluded} are left untouched. The marshalled output must be a bare jar entry — no
     * {@code <name>}, no {@code <autoIncluded>} — so the migrator's
     * {@code <dependency><mavenArtifact>...</></>} shape (asserted by the {@code openl-migrate-pomless}
     * IT regex) round-trips intact. Locks in the {@link Boolean} boxing of {@code autoIncluded}: with a
     * primitive {@code boolean} this would emit {@code <autoIncluded>false</>} despite never being set.
     */
    @Test
    void writeDependencyWithDefaultValuesOmitsAutoIncluded() {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("consumer");
        Module module = new Module();
        module.setRulesRootPath("rules/*.xlsx");
        descriptor.setModules(new ArrayList<>(List.of(module)));

        var dep = new ProjectDependencyDescriptor();
        // Bare jar entry: setAutoIncluded is intentionally NOT called — autoIncluded is a zip/OpenL
        // concept and has no meaning for a plain jar on the classpath.
        dep.setMavenArtifact("org.apache.commons:commons-text:jar:1.15.0");
        descriptor.setDependencies(new ArrayList<>(List.of(dep)));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>consumer</name>
                    <modules>
                        <module>
                            <rules-root path="rules/*.xlsx"/>
                        </module>
                    </modules>
                    <dependencies>
                        <dependency>
                            <mavenArtifact>org.apache.commons:commons-text:jar:1.15.0</mavenArtifact>
                        </dependency>
                    </dependencies>
                </project>
                """, dest);
    }

    /**
     * Explicit {@code setAutoIncluded(false)} marshals exactly like the default (no element at all):
     * the {@link ProjectDependencyDescriptor#beforeMarshal} cleanup collapses {@code false} → {@code null}
     * so that absent and explicit-false produce the same on-disk shape. The opt-in signal only ever
     * appears when it carries a non-default value ({@code true}); a redundant {@code false} would only
     * clutter migrated rules.xml files.
     */
    @Test
    void writeDependencyWithExplicitAutoIncludedFalseOmitsIt() {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("consumer");
        Module module = new Module();
        module.setRulesRootPath("rules/*.xlsx");
        descriptor.setModules(new ArrayList<>(List.of(module)));

        var dep = new ProjectDependencyDescriptor();
        dep.setName("Domain");
        dep.setAutoIncluded(false);
        descriptor.setDependencies(new ArrayList<>(List.of(dep)));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>consumer</name>
                    <modules>
                        <module>
                            <rules-root path="rules/*.xlsx"/>
                        </module>
                    </modules>
                    <dependencies>
                        <dependency>
                            <name>Domain</name>
                        </dependency>
                    </dependencies>
                </project>
                """, dest);
    }

    /**
     * Complement to {@link #writeDependencyWithDefaultValuesOmitsAutoIncluded}: an explicit
     * {@code setAutoIncluded(true)} must marshal as {@code <autoIncluded>true</autoIncluded>}. Pins
     * the round-trip for OpenL sibling entries that opt into workspace name-auto-inclusion.
     */
    @Test
    void writeDependencyWithAutoIncludedTrueEmitsIt() {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("consumer");
        Module module = new Module();
        module.setRulesRootPath("rules/*.xlsx");
        descriptor.setModules(new ArrayList<>(List.of(module)));

        var dep = new ProjectDependencyDescriptor();
        dep.setName("Domain");
        dep.setAutoIncluded(true);
        descriptor.setDependencies(new ArrayList<>(List.of(dep)));

        var dest = new String(descriptor.toBytes(), StandardCharsets.UTF_8);

        assertEquals("""
                <project>
                    <name>consumer</name>
                    <modules>
                        <module>
                            <rules-root path="rules/*.xlsx"/>
                        </module>
                    </modules>
                    <dependencies>
                        <dependency>
                            <name>Domain</name>
                            <autoIncluded>true</autoIncluded>
                        </dependency>
                    </dependencies>
                </project>
                """, dest);
    }

    private static FileSystem openZipFile(Path path) throws IOException {
        return FileSystems.newFileSystem(path, Thread.currentThread().getContextClassLoader());
    }

    private static Path toFilePath(URL url) {
        try {
            return "file".equals(url.getProtocol()) ? Path.of(url.toURI()) : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static List<Path> extractClasspathTempJars(FileSystem fs) throws Exception {
        // Kept in a separate method so the descriptor reference does not survive on the caller's frame
        // and can be garbage-collected.
        ProjectDescriptor projectDescriptor = ProjectDescriptor.read(fs.getPath("/rules-clspth.xml")).expand();
        List<Path> tempJars = new ArrayList<>();
        for (URL url : projectDescriptor.getClassPathUrls()) {
            Path path = toFilePath(url);
            if (path != null && path.getFileName().toString().startsWith("tmp-")) {
                tempJars.add(path);
            }
        }
        return tempJars;
    }

    private static void awaitDeleted(List<Path> files) throws InterruptedException {
        // Generous timeout (~10s) so the GC + Cleaner have time under CI load; the loop exits early once deleted.
        for (int i = 0; i < 200 && files.stream().anyMatch(Files::exists); i++) {
            System.gc();
            Thread.sleep(50);
        }
        files.forEach(p -> assertFalse(Files.exists(p),
                "temp jar must be deleted after the descriptor is garbage-collected: " + p));
    }
}
