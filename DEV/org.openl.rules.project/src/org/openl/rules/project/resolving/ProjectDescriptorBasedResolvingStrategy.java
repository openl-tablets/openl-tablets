package org.openl.rules.project.resolving;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.engine.OpenLCompileManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.properties.PropertiesLoader;

@Slf4j
public class ProjectDescriptorBasedResolvingStrategy implements ResolvingStrategy {

    @Override
    public boolean isRulesProject(Path folder) {
        var descriptorFile = folder.resolve(ProjectDescriptor.FILE_NAME);
        if (Files.exists(descriptorFile)) {
            log.debug("Project in folder '{}' has been resolved as project descriptor based project.",
                    descriptorFile);
            return true;
        } else {
            log.debug(
                    "Project descriptor based strategy is failed to resolve project folder '{}': there is no file '{}' in the folder.",
                    descriptorFile,
                    ProjectDescriptor.FILE_NAME);
            return false;
        }
    }

    @Override
    public ProjectDescriptor resolveProject(Path folder) throws ProjectResolvingException {
        var globalErrorMessages = new LinkedHashSet<String>();
        var propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
        try {
            var projectDescriptor = ProjectDescriptor.read(folder).expand();
            var processor = buildProcessor(propertiesFileNameProcessorBuilder, projectDescriptor, globalErrorMessages);

            var globalWarnMessages = new LinkedHashSet<String>();
            if ("org.openl.rules.project.resolving.CWPropertyFileNameProcessor"
                    .equals(projectDescriptor.getPropertiesFileNameProcessor())) {
                globalWarnMessages.add(
                        "CWPropertyFileNameProcessor is deprecated. 'CW' keyword support for 'state' property is moved to the default property processor. Remove declaration of this class from 'rules.xml'.");
            }
            for (Module module : projectDescriptor.getModules()) {
                setModuleProperties(module, processor, globalErrorMessages, globalWarnMessages);
            }
            return projectDescriptor;
        } catch (FileNotFoundException e) {
            throw new ProjectResolvingException(
                    "Project descriptor is not found. File '" + ProjectDescriptor.FILE_NAME + "' is missed.",
                    e);
        } catch (Exception e) {
            throw new ProjectResolvingException("Failed to read project descriptor.", e);
        } finally {
            propertiesFileNameProcessorBuilder.destroy();
        }
    }

    /**
     * Builds the processor of the file names the project declares.
     *
     * @return the processor, or {@code null} when it cannot be built; the reason is then added to the error messages
     */
    private static @Nullable PropertiesFileNameProcessor buildProcessor(PropertiesFileNameProcessorBuilder builder,
                                                                        ProjectDescriptor projectDescriptor,
                                                                        Set<String> errorMessages) {
        try {
            return builder.build(projectDescriptor);
        } catch (Exception e) {
            errorMessages.add(e.getMessage());
            return null;
        }
    }

    /**
     * Sets the properties a module is compiled with: the table properties its file name gives and the messages to
     * report for it.
     *
     * @param module        the module to set the properties of
     * @param processor     the processor of the file names, or {@code null} when the project has none
     * @param errorMessages the error messages of the whole project
     * @param warnMessages  the warning messages of the whole project
     */
    private static void setModuleProperties(Module module,
                                            @Nullable PropertiesFileNameProcessor processor,
                                            Set<String> errorMessages,
                                            Set<String> warnMessages) {
        var moduleErrorMessages = new HashSet<>(errorMessages);
        var moduleWarnMessages = new HashSet<>(warnMessages);
        if (module.getMethodFilter() != null
                && (!module.getMethodFilter().getIncludes().isEmpty()
                || !module.getMethodFilter().getExcludes().isEmpty())) {
            moduleWarnMessages.add(
                    "'method-filter' in the module '" + module.getName() + "' is deprecated. Use 'exposed-methods' at the project level instead.");
        }
        var params = new HashMap<String, Object>();
        if (processor != null) {
            try {
                var tableProperties = processor.process(module.getRulesRootPath());
                params.put(PropertiesLoader.EXTERNAL_MODULE_PROPERTIES_KEY, tableProperties);
            } catch (NoMatchFileNameException e) {
                moduleWarnMessages.add(e.getMessage());
            } catch (Exception | LinkageError e) {
                moduleErrorMessages.add("Failed to load custom file name processor class '" + e.getClass()
                        .getTypeName() + "': " + e.getMessage());
            }
        }
        params.put(OpenLCompileManager.ADDITIONAL_ERROR_MESSAGES_KEY, moduleErrorMessages);
        params.put(OpenLCompileManager.ADDITIONAL_WARN_MESSAGES_KEY, moduleWarnMessages);
        module.setProperties(params);
    }
}
