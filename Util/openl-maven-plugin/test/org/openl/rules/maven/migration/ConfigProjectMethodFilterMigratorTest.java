package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

class ConfigProjectMethodFilterMigratorTest {

    /** Test fixture used as a stand-in for the OpenL-generated interface. */
    interface SampleInterface {
        String greet(String name);

        int compute();
    }

    @Test
    void populatesExposedMethodsFromGeneratedInterface() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Class<?>> supplier = countingSupplier(calls, SampleInterface.class);
        ProjectDescriptor descriptor = newDescriptorWithModule(filter(Set.of(".+ greet\\(.+\\)"), null));

        ConfigProjectMethodFilterMigrator.transform(descriptor, supplier);

        assertEquals(1, calls.get());
        assertNotNull(descriptor.getExposedMethods());
        assertEquals(new TreeSet<>(List.of("compute", "greet")), descriptor.getExposedMethods().getIncludes());
        assertNull(descriptor.getExposedMethods().getExcludes());
        assertNull(descriptor.getModules().getFirst().getMethodFilter());
    }

    @Test
    void leavesExistingExposedMethodsUntouchedAndSkipsSupplier() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Class<?>> supplier = countingSupplier(calls, SampleInterface.class);
        ProjectDescriptor descriptor = newDescriptorWithModule(filter(Set.of(".+ greet\\(.+\\)"), null));
        ExposedMethods em = new ExposedMethods();
        em.setIncludes(new HashSet<>(Set.of("ping")));
        descriptor.setExposedMethods(em);

        ConfigProjectMethodFilterMigrator.transform(descriptor, supplier);

        assertEquals(0, calls.get());
        assertEquals(Set.of("ping"), descriptor.getExposedMethods().getIncludes());
        assertNull(descriptor.getModules().getFirst().getMethodFilter());
    }

    @Test
    void doesNotInvokeSupplierWhenNoMethodFilterPresent() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Class<?>> supplier = countingSupplier(calls, SampleInterface.class);
        Module module = new Module();
        module.setName("main");
        module.setRulesRootPath("rules.xlsx");
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("test");
        descriptor.setModules(new ArrayList<>(List.of(module)));

        ConfigProjectMethodFilterMigrator.transform(descriptor, supplier);

        assertEquals(0, calls.get());
    }

    @Test
    void clearsMethodFilterWhenSupplierIsNull() {
        ProjectDescriptor descriptor = newDescriptorWithModule(filter(Set.of(".+ greet\\(.+\\)"), null));

        ConfigProjectMethodFilterMigrator.transform(descriptor, null);

        assertNull(descriptor.getModules().getFirst().getMethodFilter());
        assertNull(descriptor.getExposedMethods());
    }

    @Test
    void rewritesRulesXmlOnDiskWhenMethodFilterPresent(@TempDir Path projectFolder)
            throws IOException {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Class<?>> supplier = countingSupplier(calls, SampleInterface.class);
        Path file = projectFolder.resolve("rules.xml");
        Files.writeString(file, """
                <project>
                    <name>test</name>
                    <modules>
                        <module>
                            <name>main</name>
                            <rules-root path="rules/Hello.xlsx"/>
                            <method-filter>
                                <includes>
                                    <value>.+ greet\\(.+\\)</value>
                                </includes>
                            </method-filter>
                        </module>
                    </modules>
                </project>
                """, StandardCharsets.UTF_8);

        new ConfigProjectMethodFilterMigrator().migrate(projectFolder, supplier);

        assertEquals(1, calls.get());
        assertEquals("""
                <project>
                    <name>test</name>
                    <modules>
                        <module>
                            <name>main</name>
                            <rules-root path="rules/Hello.xlsx"/>
                        </module>
                    </modules>
                    <exposed-methods>
                        <include>compute</include>
                        <include>greet</include>
                    </exposed-methods>
                </project>
                """, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void skipsWhenRulesXmlMissing(@TempDir Path projectFolder) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Class<?>> supplier = countingSupplier(calls, SampleInterface.class);

        new ConfigProjectMethodFilterMigrator().migrate(projectFolder, supplier);

        assertEquals(0, calls.get());
        assertFalse(Files.exists(projectFolder.resolve("rules.xml")));
    }

    private static Supplier<Class<?>> countingSupplier(AtomicInteger calls, Class<?> value) {
        return () -> {
            calls.incrementAndGet();
            return value;
        };
    }

    private static ProjectDescriptor newDescriptorWithModule(MethodFilter filter) {
        Module module = new Module();
        module.setName("main");
        module.setRulesRootPath("rules.xlsx");
        module.setMethodFilter(filter);
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("test");
        descriptor.setModules(new ArrayList<>(List.of(module)));
        return descriptor;
    }

    private static MethodFilter filter(Set<String> includes, Set<String> excludes) {
        MethodFilter mf = new MethodFilter();
        if (includes != null) {
            mf.setIncludes(new HashSet<>(includes));
        }
        if (excludes != null) {
            mf.setExcludes(new HashSet<>(excludes));
        }
        return mf;
    }
}
