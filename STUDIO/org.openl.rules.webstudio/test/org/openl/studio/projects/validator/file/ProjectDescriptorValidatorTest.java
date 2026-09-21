package org.openl.studio.projects.validator.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.PropertiesFileNameProcessor;
import org.openl.rules.table.properties.ITableProperties;

/**
 * The properties file name settings of a descriptor are checked against the engine: a processor class
 * the project cannot provide and a pattern the engine cannot compile are rejected, while the settings
 * real projects use are accepted. Settings the project already stores are left alone, and so is a
 * processor class of a project that is not checked out.
 */
class ProjectDescriptorValidatorTest {

    @TempDir
    private Path projectFolder;

    @Test
    void projectNameWithForbiddenCharactersIsRejected() {
        var descriptor = named();
        descriptor.setName("Rates/2026");

        var error = validate(descriptor).getFieldError("name");
        assertNotNull(error);
        assertEquals("file.descriptor.name.invalid.message", error.getCode());
    }

    @Test
    void reservedProjectNameIsRejected() {
        var descriptor = named();
        descriptor.setName("CON");

        assertTrue(validate(descriptor).hasFieldErrors("name"));
    }

    @Test
    void projectNameTheProjectAlreadyStoresIsLeftAlone() {
        var descriptor = named();
        descriptor.setName("Rates/2026");
        var stored = named();
        stored.setName("Rates/2026");

        // What the project was stored under is not the write's doing, and rejecting it would leave it unwritable.
        assertFalse(validate(descriptor, projectFolder, stored).hasFieldErrors("name"));
    }

    @Test
    void moduleNameWithForbiddenCharactersIsRejected() {
        var errors = validate(withModules(module("Rates/2026", "*.xlsx")));

        var error = errors.getFieldError("modules[0].name");
        assertNotNull(error);
        assertEquals("file.descriptor.module.name.invalid.message", error.getCode());
    }

    @Test
    void twoModulesOfOneNameAreRejectedOnTheSecond() {
        var errors = validate(withModules(module("Rates", "*.xlsx"), module("Rates", "rules/*.xlsx")));

        assertFalse(errors.hasFieldErrors("modules[0].name"));
        assertTrue(errors.hasFieldErrors("modules[1].name"));
    }

    @Test
    void moduleRepeatingTheNameOfOneTheProjectStoresIsRejected() {
        var stored = withModules(module("Rates", "*.xlsx"));

        // The stored module is left alone, so the repeat is reported on the module the write declares —
        // wherever in the list it was written.
        var errors = validate(withModules(module("Rates", "rules/*.xlsx"), module("Rates", "*.xlsx")),
                projectFolder, stored);

        assertTrue(errors.hasFieldErrors("modules[0].name"));
        assertFalse(errors.hasFieldErrors("modules[1].name"));
    }

    @Test
    void moduleWithoutAPathIsRejected() {
        var errors = validate(withModules(module("Rates", null)));

        var error = errors.getFieldError("modules[0].rulesRootPath");
        assertNotNull(error);
        assertEquals("file.descriptor.module.path.required.message", error.getCode());
    }

    @Test
    void moduleWithNeitherNameNorPathIsRejectedWithoutNamingIt() {
        var error = validate(withModules(module(null, null))).getFieldError("modules[0].rulesRootPath");

        // A module with no name of its own is not called 'null' in the message the author is given.
        assertNotNull(error);
        assertEquals("file.descriptor.module.path.required.unnamed.message", error.getCode());
    }

    @Test
    void modulePointingAtAMissingFileIsRejected() {
        var errors = validate(withModules(module("Rates", "rules/Rates.xlsx")));

        var error = errors.getFieldError("modules[0].rulesRootPath");
        assertNotNull(error);
        assertEquals("file.descriptor.module.path.not-found.message", error.getCode());
    }

    @Test
    void modulePointingAtAFileTheProjectHoldsIsAccepted() throws Exception {
        Files.createDirectories(projectFolder.resolve("rules"));
        Files.createFile(projectFolder.resolve("rules/Rates.xlsx"));

        assertFalse(validate(withModules(module("Rates", "rules/Rates.xlsx"))).hasErrors());
    }

    @Test
    void modulePathIsNotLookedForWhereThereIsNoWorkingCopy() {
        // A project that is not checked out has no files to look in, and a path is not wrong for that.
        assertFalse(validate(withModules(module("Rates", "rules/Rates.xlsx")), null, null).hasErrors());
    }

    @Test
    void pathAlreadyReadByAnotherModuleIsRejected() {
        var errors = validate(withModules(module("Everything", "*.xlsx"), module("Again", "*.xlsx")));

        assertTrue(errors.hasFieldErrors("modules[1].rulesRootPath"));
    }

    @Test
    void pathAPatternOfAnotherModuleAlreadyMatchesIsRejected() throws Exception {
        Files.createFile(projectFolder.resolve("Rates.xlsx"));

        var errors = validate(withModules(module("Everything", "*.xlsx"), module("Rates", "Rates.xlsx")));

        assertTrue(errors.hasFieldErrors("modules[1].rulesRootPath"));
    }

    @Test
    void patternOverAPathAnotherModuleAlreadyReadsIsRejected() throws Exception {
        Files.createFile(projectFolder.resolve("Rates.xlsx"));

        // The same two modules the other way round: the rules are read twice whichever of them is written first.
        var errors = validate(withModules(module("Rates", "Rates.xlsx"), module("Everything", "*.xlsx")));

        assertTrue(errors.hasFieldErrors("modules[1].rulesRootPath"));
    }

    @Test
    void pathLeadingOutOfTheProjectIsRejected() throws Exception {
        var project = Files.createDirectory(projectFolder.resolve("project"));
        Files.createFile(projectFolder.resolve("Rates.xlsx"));

        // The file is there, beside the project rather than in it: a module reads what the project holds.
        var errors = validate(withModules(module("Rates", "../Rates.xlsx")), project, null);

        var error = errors.getFieldError("modules[0].rulesRootPath");
        assertNotNull(error);
        assertEquals("file.descriptor.module.path.not-found.message", error.getCode());
    }

    @Test
    void moduleTheProjectAlreadyStoresIsLeftAlone() {
        var stored = withModules(module("Rates/2026", "rules/Rates.xlsx"));

        // The module is stored as it is written, so the write did not introduce what is wrong with it.
        assertFalse(validate(withModules(module("Rates/2026", "rules/Rates.xlsx")), projectFolder, stored).hasErrors());
    }

    @Test
    void moduleWithoutANameIsAccepted() throws Exception {
        Files.createFile(projectFolder.resolve("Rates.xlsx"));

        // The engine names such a module after the file its path points at, and a pattern after what it matched.
        assertFalse(validate(withModules(module(null, "Rates.xlsx"), module(null, "rules/*.xlsx"))).hasErrors());
    }

    @Test
    void supportsProjectDescriptor() {
        assertTrue(new ProjectDescriptorValidator(null, null).supports(ProjectDescriptor.class));
    }

    @Test
    void unknownProcessorClassIsRejected() {
        var errors = validate(descriptorWithProcessor("aaa"));

        var error = errors.getFieldError("propertiesFileNameProcessor");
        assertNotNull(error);
        assertEquals("file.descriptor.processor.invalid.message", error.getCode());
    }

    @Test
    void processorThatIsNotAProcessorIsRejected() {
        assertTrue(validate(descriptorWithProcessor(String.class.getName()))
                .hasFieldErrors("propertiesFileNameProcessor"));
    }

    @Test
    void wellKnownProcessorIsAccepted() {
        var descriptor = descriptorWithProcessor("org.openl.rules.project.resolving.CWPropertyFileNameProcessor");
        descriptor.setPropertiesFileNamePatterns(new String[]{"%lob%-%state%"});

        assertFalse(validate(descriptor).hasErrors());
    }

    @Test
    void unknownPropertyInPatternIsRejected() {
        var errors = validate(descriptorWithPatterns("%unknownProperty%"));

        var error = errors.getFieldError("propertiesFileNamePatterns");
        assertNotNull(error);
        assertEquals("file.descriptor.pattern.invalid.message", error.getCode());
    }

    @Test
    void propertyDeclaredTwiceInPatternIsRejected() {
        assertTrue(validate(descriptorWithPatterns("%lob%-%lob%")).hasFieldErrors("propertiesFileNamePatterns"));
    }

    @Test
    void invalidDateFormatInPatternIsRejected() {
        assertTrue(validate(descriptorWithPatterns("%effectiveDate:ttt%"))
                .hasFieldErrors("propertiesFileNamePatterns"));
    }

    /**
     * A pattern carrying no property is a plain file name mask: it assigns nothing, matches the modules
     * named after it, and is used by real projects.
     */
    @Test
    void patternWithoutPropertiesIsAccepted() {
        assertFalse(validate(descriptorWithPatterns("%lob%-%state%", "Tests-*", "DataTables")).hasErrors());
    }

    @Test
    void descriptorWithoutFileNameSettingsIsAccepted() {
        assertFalse(validate(named()).hasErrors());
    }

    @Test
    void descriptorWithoutANameIsRejected() {
        var error = validate(new ProjectDescriptor()).getFieldError("name");
        assertNotNull(error);
        assertEquals("file.descriptor.name.required.message", error.getCode());
    }

    /**
     * A pattern needs no classpath, so it is checked whether the project is checked out or not.
     */
    @Test
    void patternOfProjectThatIsNotCheckedOutIsRejected() {
        assertTrue(validate(descriptorWithPatterns("%unknownProperty%"), null, null)
                .hasFieldErrors("propertiesFileNamePatterns"));
    }

    /**
     * The processor class may be packed into a library of the project, which only a checked out project
     * lets the engine read. Such a descriptor is left alone instead of being rejected.
     */
    @Test
    void processorOfProjectThatIsNotCheckedOutIsAccepted() {
        assertFalse(validate(descriptorWithProcessor("com.acme.MissingProcessor"), null, null).hasErrors());
    }

    /**
     * A descriptor rewritten for another reason keeps the settings the project stores, and they are not
     * checked again — an inherited defect must not block an unrelated write.
     */
    @Test
    void settingsTheProjectAlreadyStoresAreAccepted() {
        var stored = descriptorWithProcessor("aaa");

        assertFalse(validate(descriptorWithProcessor("aaa"), projectFolder, stored).hasErrors());
    }

    @Test
    void changedSettingsAreCheckedAgainstTheStoredOnes() {
        var stored = descriptorWithProcessor("aaa");

        assertTrue(validate(descriptorWithProcessor("bbb"), projectFolder, stored)
                .hasFieldErrors("propertiesFileNameProcessor"));
    }

    /**
     * A migrate drops the discontinued processor and keeps the patterns: the changed processor is
     * checked, while the patterns the project already stored are not.
     */
    @Test
    void patternsKeptByAWriteThatChangesTheProcessorAreNotRejected() {
        var stored = descriptorWithProcessor("org.openl.rules.project.resolving.CWPropertyFileNameProcessor");
        stored.setPropertiesFileNamePatterns(new String[]{"%unknownProperty%"});

        assertFalse(validate(descriptorWithPatterns("%unknownProperty%"), projectFolder, stored).hasErrors());
    }

    /**
     * The engine rethrows what a custom processor raises while it is built; that is a setting the
     * project cannot use, not a server fault.
     */
    @Test
    void processorFailingWhileItIsBuiltIsRejected() {
        var descriptor = descriptorWithProcessor(RejectingProcessor.class.getName());
        descriptor.setPropertiesFileNamePatterns(new String[]{"%lob%"});

        assertTrue(validate(descriptor).hasFieldErrors("propertiesFileNameProcessor"));
    }

    private Errors validate(ProjectDescriptor descriptor) {
        return validate(descriptor, projectFolder, null);
    }

    private static Errors validate(ProjectDescriptor descriptor, Path folder, ProjectDescriptor stored) {
        var errors = new BeanPropertyBindingResult(descriptor, "descriptor");
        new ProjectDescriptorValidator(folder, stored).validate(descriptor, errors);
        return errors;
    }

    private static ProjectDescriptor descriptorWithProcessor(String processor) {
        var descriptor = named();
        descriptor.setPropertiesFileNameProcessor(processor);
        return descriptor;
    }

    private static ProjectDescriptor descriptorWithPatterns(String... patterns) {
        var descriptor = named();
        descriptor.setPropertiesFileNamePatterns(patterns);
        return descriptor;
    }

    /** A descriptor carrying what every descriptor carries, so a test says only what it is about. */
    private static ProjectDescriptor named() {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("Rating");
        return descriptor;
    }

    private static ProjectDescriptor withModules(Module... modules) {
        var descriptor = named();
        descriptor.setModules(List.of(modules));
        return descriptor;
    }

    private static Module module(String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        return module;
    }

    /**
     * A processor that refuses the pattern it is given, the way a custom processor reports one it does
     * not accept: with an unchecked exception the engine rethrows.
     */
    public static class RejectingProcessor implements PropertiesFileNameProcessor {

        public RejectingProcessor(String pattern) {
            throw new IllegalArgumentException("Unsupported pattern '%s'.".formatted(pattern));
        }

        @Override
        public ITableProperties process(String modulePath) {
            throw new UnsupportedOperationException();
        }
    }
}
