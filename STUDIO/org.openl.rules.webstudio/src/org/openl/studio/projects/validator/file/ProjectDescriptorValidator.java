package org.openl.studio.projects.validator.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.InvalidFileNamePatternException;
import org.openl.rules.project.resolving.InvalidFileNameProcessorException;
import org.openl.rules.project.resolving.PropertiesFileNameProcessorBuilder;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Validator for a project descriptor ({@code rules.xml}) written to a project.
 *
 * <p>What the Editor checked field by field is checked here on the file itself: the project's name, every
 * module's name and path, and the properties file name settings. The descriptor is written as text and sent
 * as a file, so this is the only place left that can tell the author what the engine will not read.
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

    private static final String NAME_FIELD = "name";
    private static final String PATH_FIELD = "rulesRootPath";
    private static final String MODULES_FIELD = "modules";
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
        validateName(descriptor, errors);
        validateModules(descriptor, errors);
        validateFileNameSettings(descriptor, errors);
    }

    /**
     * The name the project is known by, which names its folder in a repository.
     *
     * <p>Checked only where the write changes it: a project stored under a name no longer allowed keeps it
     * until someone renames it, and a descriptor rewritten for another reason must not fail on it.
     */
    private void validateName(ProjectDescriptor descriptor, Errors errors) {
        var name = descriptor.getName();
        if (stored != null && Objects.equals(stored.getName(), name)) {
            return;
        }
        if (StringUtils.isBlank(name)) {
            errors.rejectValue(NAME_FIELD, "file.descriptor.name.required.message", null, null);
        } else if (!NameChecker.checkName(name)) {
            errors.rejectValue(NAME_FIELD, "file.descriptor.name.invalid.message",
                    new Object[]{name, NameChecker.getForbiddenCharacters()}, null);
        } else if (NameChecker.isReservedName(name)) {
            errors.rejectValue(NAME_FIELD, "file.descriptor.name.reserved.message", new Object[]{name}, null);
        }
    }

    /**
     * Every module the write adds or changes: what it is called, and where its rules are read from.
     *
     * <p>A module standing for a pattern needs no name of its own — the modules it stands for are named after
     * the files it matched — and names no single file, so there is no file to look for. A module naming one
     * file is checked against the working copy where there is one to check against.
     *
     * <p>Only what the write changes is checked, against what the project stores: a project whose descriptor
     * already names a module that cannot be read stays writable, and the module registered by a table being
     * created does not carry the rest of the file through validation with it.
     */
    private void validateModules(ProjectDescriptor descriptor, Errors errors) {
        var modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            return;
        }
        for (var index = 0; index < modules.size(); index++) {
            var module = modules.get(index);
            if (isStored(module)) {
                continue;
            }
            validateModuleName(module, modules, index, errors);
            validateModulePath(module, modules, index, errors);
        }
    }

    /**
     * What a module is called.
     *
     * <p>A module that declares no name is left alone: the engine names it after the file its path points at,
     * and the modules a pattern stands for after the files it matched. A name that is declared has to be one a
     * repository can hold, and has to be the module's own — two modules of one name are one module to the
     * engine, and the second would quietly replace the first.
     */
    private void validateModuleName(Module module, List<Module> modules, int index, Errors errors) {
        var name = module.getName();
        if (StringUtils.isBlank(name)) {
            return;
        }
        if (!NameChecker.checkName(name)) {
            errors.rejectValue(field(index, NAME_FIELD), "file.descriptor.module.name.invalid.message",
                    new Object[]{name, NameChecker.getForbiddenCharacters()}, null);
        } else if (repeated(modules, index, other -> name.equals(other.getName()))) {
            errors.rejectValue(field(index, NAME_FIELD), "file.descriptor.module.name.duplicate.message",
                    new Object[]{name}, null);
        }
    }

    /**
     * Where a module reads its rules from.
     *
     * <p>A path naming one file is looked for in the working copy: a module pointing at a file the project does
     * not hold compiles into nothing. A pattern names no file to look for, and is checked only against the
     * other modules — a path already read by another module, or falling under another module's pattern, is the
     * same rules read twice, whichever of the two is declared first.
     */
    private void validateModulePath(Module module, List<Module> modules, int index, Errors errors) {
        var path = module.getRulesRootPath();
        if (StringUtils.isBlank(path)) {
            rejectMissingPath(module, index, errors);
        } else if (!module.isModuleWithWildcard() && !isHeldByProject(path)) {
            errors.rejectValue(field(index, PATH_FIELD), "file.descriptor.module.path.not-found.message",
                    new Object[]{path}, null);
        } else if (repeated(modules, index, other -> readTheSame(module, other))) {
            errors.rejectValue(field(index, PATH_FIELD), "file.descriptor.module.path.duplicate.message",
                    new Object[]{path}, null);
        }
    }

    /**
     * A module declaring no path reads nothing.
     *
     * <p>Named where it carries a name of its own: a module is otherwise named after the file it reads, and
     * this one names no file to be named after.
     */
    private static void rejectMissingPath(Module module, int index, Errors errors) {
        var name = module.getName();
        if (StringUtils.isBlank(name)) {
            errors.rejectValue(field(index, PATH_FIELD),
                    "file.descriptor.module.path.required.unnamed.message", null, null);
        } else {
            errors.rejectValue(field(index, PATH_FIELD),
                    "file.descriptor.module.path.required.message", new Object[]{name}, null);
        }
    }

    /** The module's field, named so the reader is told which of them the write is refused over. */
    private static String field(int index, String name) {
        return "%s[%d].%s".formatted(MODULES_FIELD, index, name);
    }

    /**
     * Whether the working copy holds the file the path names.
     *
     * <p>A project that is not checked out has nothing to look in, and a path is not wrong for that. A path
     * leading out of the project — up through its parent, or from the root of the disk — names a file the
     * project does not hold, whatever stands there.
     */
    private boolean isHeldByProject(String path) {
        if (projectFolder == null) {
            return true;
        }
        var folder = projectFolder.normalize();
        var file = folder.resolve(path.replace('\\', '/')).normalize();
        return file.startsWith(folder) && Files.exists(file);
    }

    /**
     * Whether another module of the descriptor already carries what this one declares.
     *
     * <p>Reported once, and on a module the write can move: the later of two the descriptor declares, and the
     * declared one where the other is already stored — a stored module is left alone and would report nothing.
     */
    private boolean repeated(List<Module> modules, int index, Predicate<Module> carries) {
        return IntStream.range(0, modules.size())
                .filter(other -> other != index)
                .filter(other -> carries.test(modules.get(other)))
                .anyMatch(other -> other < index || isStored(modules.get(other)));
    }

    /** Whether two modules read the same rules — the same path, or one module's pattern over the other's. */
    private static boolean readTheSame(Module one, Module other) {
        var onePath = one.getRulesRootPath();
        var otherPath = other.getRulesRootPath();
        if (onePath == null || otherPath == null) {
            return false;
        }
        return Objects.equals(onePath, otherPath) || matches(one, otherPath) || matches(other, onePath);
    }

    /** Whether the module stands for a pattern the path falls under. */
    private static boolean matches(Module pattern, String path) {
        return pattern.isModuleWithWildcard()
                && FileUtils.pathMatches(pattern.getRulesRootPath(), path.replace('\\', '/'));
    }

    /** Whether the project already stores this module as it is written, so the write leaves it alone. */
    private boolean isStored(Module module) {
        if (stored == null || CollectionUtils.isEmpty(stored.getModules())) {
            return false;
        }
        return stored.getModules().stream().anyMatch(kept -> Objects.equals(kept.getName(), module.getName())
                && Objects.equals(kept.getRulesRootPath(), module.getRulesRootPath()));
    }

    private void validateFileNameSettings(ProjectDescriptor descriptor, Errors errors) {
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
