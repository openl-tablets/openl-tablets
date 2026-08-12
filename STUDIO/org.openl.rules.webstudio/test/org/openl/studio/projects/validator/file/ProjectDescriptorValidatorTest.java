package org.openl.studio.projects.validator.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

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
        assertFalse(validate(new ProjectDescriptor()).hasErrors());
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
        var descriptor = new ProjectDescriptor();
        descriptor.setPropertiesFileNameProcessor(processor);
        return descriptor;
    }

    private static ProjectDescriptor descriptorWithPatterns(String... patterns) {
        var descriptor = new ProjectDescriptor();
        descriptor.setPropertiesFileNamePatterns(patterns);
        return descriptor;
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
