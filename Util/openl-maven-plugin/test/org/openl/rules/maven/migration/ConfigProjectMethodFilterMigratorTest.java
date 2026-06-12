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

    /** Stand-in for a generated interface with prefix-named, exact-named and unfoldable methods. */
    interface ApiInterface {
        void _api_deduct(String policy);

        void _api_pay(String policy);

        double premium();

        String getProcess1(int step);
    }

    /** Stand-in for a generated interface where every method shares the same name prefix. */
    interface PrefixOnlyInterface {
        void _api_deduct(String policy);

        void _api_pay(String policy);
    }

    /** Stand-in for a generated interface with a single method. */
    interface PremiumInterface {
        double premium(String policy);
    }

    /** Stand-in for a generated interface with a zero-argument method sharing a prefix with another one. */
    interface ComputeInterface {
        int compute();

        int computeAll(int max);
    }

    /** Stand-in for a generated interface whose methods differ in argument types. */
    interface RestrictedInterface {
        void _api_deduct(int amount);

        void _api_pay(String policy);
    }

    /** Stand-in for a generated interface with two unrelated methods. */
    interface MixedInterface {
        void _api_deduct(String policy);

        double premium(String policy);
    }

    /** Stand-in for a generated interface without methods. */
    interface EmptyInterface {
    }

    /** Stand-in for a generated interface with a digit-suffixed method name. */
    interface ProcessInterface {
        String getProcess1(int step);
    }

    @Test
    void populatesExposedMethodsFromGeneratedInterface() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Class<?>> supplier = countingSupplier(calls, SampleInterface.class);
        ProjectDescriptor descriptor = newDescriptorWithModule(filter(Set.of(".+ greet\\(.+\\)"), null));

        ConfigProjectMethodFilterMigrator.transform(descriptor, supplier);

        assertEquals(1, calls.get());
        assertNotNull(descriptor.getExposedMethods());
        assertEquals(Set.of("compute", "greet"), descriptor.getExposedMethods().getIncludes());
        assertNull(descriptor.getExposedMethods().getExcludes());
        assertNull(descriptor.getModules().getFirst().getMethodFilter());
    }

    @Test
    void convertsPrefixPatternsToGlobsAndUnfoldsTheRest() {
        assertIncludes(ApiInterface.class, filter(Set.of(
                ".+ _api_.+\\(.+\\)",
                ".*premium.*",
                ".*getProcess.*",
                ".+ external.+\\(.*\\)"), null),
                "_api_*", "getProcess1", "premium");
    }

    @Test
    void dropsPatternsCoveredByGlob() {
        assertIncludes(PrefixOnlyInterface.class, filter(Set.of(
                ".+ _api_.+\\(.+\\)",
                ".*_api_deduct.*"), null),
                "_api_*");
    }

    @Test
    void exactNamePatternBecomesPlainName() {
        assertIncludes(PremiumInterface.class, filter(Set.of(".* premium\\(.*\\)"), null), "premium");
    }

    @Test
    void treatsDotPlusAndDotStarAsEqual() {
        // .+ between the parentheses would not match the zero-argument compute() — as .* it does, so the
        // prefix glob covers both methods and the pattern collapses to comp*.
        assertIncludes(ComputeInterface.class, filter(Set.of(".+ comp.+\\(.+\\)"), null), "comp*");
    }

    @Test
    void matchAllPatternBecomesStarGlob() {
        assertIncludes(ApiInterface.class, filter(Set.of(".*"), null), "*");
    }

    @Test
    void matchAllSignatureShapeCoversEveryOtherPattern() {
        // '.+ .+\(.+\)' constrains nothing but the signature shape every method has, so it collapses to
        // the bare '*' glob, which then eats the narrower '_api_*'.
        assertIncludes(ApiInterface.class, filter(Set.of(
                ".+ .+\\(.+\\)",
                ".+ _api_.+\\(.+\\)"), null),
                "*");
    }

    @Test
    void enumeratesWhenPrefixPatternRestrictsArguments() {
        assertIncludes(RestrictedInterface.class, filter(Set.of(".+ _api_.+\\(int\\)"), null),
                "_api_deduct", "_api_pay");
    }

    @Test
    void enumeratesAllMethodsWhenExcludesPresent() {
        assertIncludes(PrefixOnlyInterface.class,
                filter(Set.of(".+ _api_.+\\(.+\\)"), Set.of(".* hidden\\(.*\\)")),
                "_api_deduct", "_api_pay");
    }

    @Test
    void excludesOnlyFilterEnumeratesMethods() {
        assertIncludes(SampleInterface.class, filter(null, Set.of(".* internal\\(.*\\)")), "compute", "greet");
    }

    @Test
    void unfoldsComplexRegexpsIntoMethodNames() {
        assertIncludes(MixedInterface.class, filter(Set.of(".+ (_api_deduct|premium)\\(.*\\)"), null),
                "_api_deduct", "premium");
    }

    @Test
    void ignoresInvalidRegexpsButKeepsMethodsExposed() {
        assertIncludes(SampleInterface.class, filter(Set.of("[", ".+ greet\\(.+\\)"), null), "compute", "greet");
    }

    @Test
    void blankIncludePatternsAreIgnored() {
        ProjectDescriptor descriptor = newDescriptorWithModule(filter(Set.of("   "), null));

        ConfigProjectMethodFilterMigrator.transform(descriptor, () -> SampleInterface.class);

        assertEquals(Set.of("compute", "greet"), descriptor.getExposedMethods().getIncludes());
        assertNull(descriptor.getModules().getFirst().getMethodFilter());
    }

    @Test
    void unionsFiltersAcrossModules() {
        ProjectDescriptor descriptor = newDescriptorWithModules(
                filter(Set.of(".*_api_deduct.*"), null),
                filter(Set.of(".+ _api_.+\\(.+\\)"), null));

        ConfigProjectMethodFilterMigrator.transform(descriptor, () -> PrefixOnlyInterface.class);

        assertEquals(Set.of("_api_*"), descriptor.getExposedMethods().getIncludes());
        assertNull(descriptor.getModules().get(0).getMethodFilter());
        assertNull(descriptor.getModules().get(1).getMethodFilter());
    }

    @Test
    void skipsDescriptorWithoutModules() {
        AtomicInteger calls = new AtomicInteger();
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("test");

        ConfigProjectMethodFilterMigrator.transform(descriptor, countingSupplier(calls, SampleInterface.class));

        assertEquals(0, calls.get());
        assertNull(descriptor.getExposedMethods());
    }

    @Test
    void matchesRegexClassesWithoutGlobbing() {
        assertIncludes(ProcessInterface.class, filter(Set.of(".+ getProcess\\d\\(.*\\)"), null), "getProcess1");
    }

    @Test
    void enumeratesWhenLiteralIsNotNamePrefix() {
        // 'emiu' matches inside the name, not from its start — a glob cannot be derived.
        assertIncludes(PremiumInterface.class, filter(Set.of(".+emiu.+\\(.+\\)"), null), "premium");
    }

    @Test
    void clearsMethodFilterWhenSupplierReturnsNull() {
        ProjectDescriptor descriptor = newDescriptorWithModule(filter(Set.of(".+ greet\\(.+\\)"), null));

        ConfigProjectMethodFilterMigrator.transform(descriptor, () -> null);

        assertNull(descriptor.getModules().getFirst().getMethodFilter());
        assertNull(descriptor.getExposedMethods());
    }

    @Test
    void writesNoExposedMethodsWhenInterfaceHasNoMethods() {
        ProjectDescriptor descriptor = newDescriptorWithModule(filter(Set.of(".+ greet\\(.+\\)"), null));

        ConfigProjectMethodFilterMigrator.transform(descriptor, () -> EmptyInterface.class);

        assertNull(descriptor.getModules().getFirst().getMethodFilter());
        assertNull(descriptor.getExposedMethods());
    }

    @Test
    void leavesExposedMethodsWithOnlyExcludesUntouched() {
        AtomicInteger calls = new AtomicInteger();
        ProjectDescriptor descriptor = newDescriptorWithModule(filter(Set.of(".+ greet\\(.+\\)"), null));
        ExposedMethods em = new ExposedMethods();
        em.setExcludes(new HashSet<>(Set.of("internal*")));
        descriptor.setExposedMethods(em);

        ConfigProjectMethodFilterMigrator.transform(descriptor, countingSupplier(calls, SampleInterface.class));

        assertEquals(0, calls.get());
        assertNull(descriptor.getExposedMethods().getIncludes());
        assertEquals(Set.of("internal*"), descriptor.getExposedMethods().getExcludes());
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
    void rewritesRulesXmlWithGlobPatterns(@TempDir Path projectFolder) throws IOException {
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
                                    <value>.+ _api_.+\\(.+\\)</value>
                                    <value>.*premium.*</value>
                                    <value>.*getProcess.*</value>
                                    <value>.+ external.+\\(.*\\)</value>
                                </includes>
                            </method-filter>
                        </module>
                    </modules>
                </project>
                """, StandardCharsets.UTF_8);

        new ConfigProjectMethodFilterMigrator().migrate(projectFolder, () -> ApiInterface.class);

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
                        <include>_api_*</include>
                        <include>getProcess1</include>
                        <include>premium</include>
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

    /** Runs the migration against the interface and filter and asserts the produced include patterns. */
    private static void assertIncludes(Class<?> generatedInterface, MethodFilter filter, String... expected) {
        ProjectDescriptor descriptor = newDescriptorWithModule(filter);

        ConfigProjectMethodFilterMigrator.transform(descriptor, () -> generatedInterface);

        assertEquals(Set.of(expected), descriptor.getExposedMethods().getIncludes());
    }

    private static Supplier<Class<?>> countingSupplier(AtomicInteger calls, Class<?> value) {
        return () -> {
            calls.incrementAndGet();
            return value;
        };
    }

    private static ProjectDescriptor newDescriptorWithModule(MethodFilter filter) {
        return newDescriptorWithModules(filter);
    }

    private static ProjectDescriptor newDescriptorWithModules(MethodFilter... filters) {
        List<Module> modules = new ArrayList<>(filters.length);
        for (MethodFilter filter : filters) {
            Module module = new Module();
            module.setName("module" + (modules.size() + 1));
            module.setRulesRootPath("rules" + (modules.size() + 1) + ".xlsx");
            module.setMethodFilter(filter);
            modules.add(module);
        }
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName("test");
        descriptor.setModules(modules);
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
