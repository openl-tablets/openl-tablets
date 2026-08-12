package org.openl.studio.projects.validator.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.InvalidFileNamePatternException;
import org.openl.rules.project.resolving.InvalidFileNameProcessorException;
import org.openl.rules.project.resolving.PropertiesFileNameProcessorBuilder;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Validator for the properties file name settings of a project descriptor ({@code rules.xml}).
 *
 * <p>The settings are checked with the engine itself: the processor class must be loadable from the
 * project, and every pattern must name existing properties in a form the engine can compile. Invalid
 * settings are reported when the descriptor is saved instead of surfacing later as a compilation
 * failure.
 *
 * <p>Only the settings a write changes are checked, so a descriptor rewritten for another reason - a
 * module registered, a project migrated - is never rejected for settings it inherited.
 *
 * <p>The processor class is looked up on the project's own classpath, which covers the libraries the
 * project ships. A project that is not checked out has no working copy to read them from, so a
 * declared processor - and the patterns that processor interprets - are left alone rather than
 * reported as unusable.
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class ProjectDescriptorValidator implements Validator {

    private static final String PROCESSOR_FIELD = "propertiesFileNameProcessor";
    private static final String PATTERNS_FIELD = "propertiesFileNamePatterns";

    /**
     * The folder of the project's working copy, or {@code null} when the project is not checked out.
     */
    private final @Nullable Path projectFolder;

    /**
     * The descriptor the project stores, or {@code null} when it has none yet.
     */
    private final @Nullable ProjectDescriptor stored;

    /**
     * Builds a validator for a descriptor written to the project, checking it against the descriptor
     * the project stores now.
     */
    public static ProjectDescriptorValidator forProject(RulesProject project, @Nullable ProjectDescriptor stored) {
        return new ProjectDescriptorValidator(localProjectFolder(project), stored);
    }

    private static @Nullable Path localProjectFolder(RulesProject project) {
        var localFolderName = project.getLocalFolderName();
        if (localFolderName == null) {
            return null;
        }
        var folder = project.getLocalRepository().getRoot().resolve(localFolderName);
        return Files.isDirectory(folder) ? folder : null;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ProjectDescriptor.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var descriptor = (ProjectDescriptor) target;
        var processorChanged = isProcessorChanged(descriptor);
        var patternsChanged = arePatternsChanged(descriptor);
        if (!processorChanged && !patternsChanged || !isCheckable(descriptor)) {
            return;
        }
        descriptor.setProjectFolder(projectFolder);
        var processorBuilder = new PropertiesFileNameProcessorBuilder();
        try {
            processorBuilder.build(descriptor);
        } catch (InvalidFileNameProcessorException e) {
            reject(errors, PROCESSOR_FIELD, processorChanged, e);
        } catch (InvalidFileNamePatternException e) {
            reject(errors, PATTERNS_FIELD, patternsChanged, e);
        } catch (RuntimeException e) {
            // The engine rethrows whatever a custom processor raises while it is built, and wraps a failure
            // to read the project's classpath. Settings that cannot be built are rejected, not a server fault.
            var declaresProcessor = StringUtils.isNotBlank(descriptor.getPropertiesFileNameProcessor());
            reject(errors, declaresProcessor ? PROCESSOR_FIELD : PATTERNS_FIELD,
                    declaresProcessor ? processorChanged : patternsChanged, e);
        } finally {
            processorBuilder.destroy();
            descriptor.releaseClassPath();
        }
    }

    /**
     * Reports the failure on the field, unless the write leaves that field as the project stores it - an
     * inherited defect must not reject a write that did not introduce it.
     */
    private static void reject(Errors errors, String field, boolean changed, Exception failure) {
        if (!changed) {
            return;
        }
        var code = PROCESSOR_FIELD.equals(field)
                ? "file.descriptor.processor.invalid.message"
                : "file.descriptor.pattern.invalid.message";
        var message = failure.getMessage() == null ? failure.toString() : failure.getMessage();
        errors.rejectValue(field, code, new Object[]{message}, null);
    }

    private boolean isProcessorChanged(ProjectDescriptor descriptor) {
        return stored == null || !Objects.equals(stored.getPropertiesFileNameProcessor(),
                descriptor.getPropertiesFileNameProcessor());
    }

    private boolean arePatternsChanged(ProjectDescriptor descriptor) {
        return stored == null
                || !Arrays.equals(stored.getPropertiesFileNamePatterns(), descriptor.getPropertiesFileNamePatterns());
    }

    /**
     * Whether the settings can be checked: something must be declared, and a declared processor class -
     * which also interprets the patterns - needs the project's classpath, which only a checked out
     * project provides.
     */
    private boolean isCheckable(ProjectDescriptor descriptor) {
        if (StringUtils.isNotBlank(descriptor.getPropertiesFileNameProcessor())) {
            return projectFolder != null;
        }
        return CollectionUtils.isNotEmpty(descriptor.getPropertiesFileNamePatterns());
    }
}
