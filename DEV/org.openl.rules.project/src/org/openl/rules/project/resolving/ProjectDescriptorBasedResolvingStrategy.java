package org.openl.rules.project.resolving;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import lombok.extern.slf4j.Slf4j;

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
            PropertiesFileNameProcessor processor = null;
            try {
                processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
            } catch (Exception e) {
                globalErrorMessages.add(e.getMessage());
            }

            var globalWarnMessages = new LinkedHashSet<String>();
            if ("org.openl.rules.project.resolving.CWPropertyFileNameProcessor"
                    .equals(projectDescriptor.getPropertiesFileNameProcessor())) {
                globalWarnMessages.add(
                        "CWPropertyFileNameProcessor is deprecated. 'CW' keyword support for 'state' property is moved to the default property processor. Remove declaration of this class from 'rules.xml'.");
            }
            for (Module module : projectDescriptor.getModules()) {
                var moduleErrorMessages = new HashSet<String>(globalErrorMessages);
                var moduleWarnMessages = new HashSet<String>(globalWarnMessages);
                if (module.getMethodFilter() != null
                        && (!module.getMethodFilter().getIncludes().isEmpty()
                        || !module.getMethodFilter().getExcludes().isEmpty())) {
                    moduleWarnMessages.add(
                            "'method-filter' in the module '" + module.getName() + "' is deprecated. Use 'exposed-methods' at the project level instead.");
                }
                var params = new HashMap<String, Object>();
                if (processor != null) {
                    try {
                        final var relativePath = module.getRulesRootPath();
                        var tableProperties = processor.process(relativePath);
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
}
