package org.openl.rules.project.resolving;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.openl.engine.OpenLCompileManager;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectDescriptorBasedResolvingStrategy implements ResolvingStrategy {

    public static final String PROJECT_DESCRIPTOR_FILE_NAME = "rules.xml";
    private static final Logger LOG = LoggerFactory.getLogger(ProjectDescriptorBasedResolvingStrategy.class);

    @Override
    public boolean isRulesProject(File folder) {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        if (descriptorFile.exists()) {
            LOG.debug("Project in folder '{}' has been resolved as project descriptor based project.",
                folder.getPath());
            return true;
        } else {
            LOG.debug(
                "Project descriptor based strategy is failed to resolve project folder '{}': there is no file '{}' in the folder.",
                folder.getPath(),
                PROJECT_DESCRIPTOR_FILE_NAME);
            return false;
        }
    }

    @Override
    public ProjectDescriptor resolveProject(File folder) throws ProjectResolvingException {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        ProjectDescriptorManager descriptorManager = new ProjectDescriptorManager();
        Set<String> globalErrorMessages = new LinkedHashSet<>();
        PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
        try {
            ProjectDescriptor projectDescriptor = descriptorManager.readDescriptor(descriptorFile);
            PropertiesFileNameProcessor processor = null;
            try {
                processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
            } catch (Exception e) {
                globalErrorMessages.add(e.getMessage());
            }

            Set<String> globalWarnMessages = new LinkedHashSet<>();
            if ("org.openl.rules.project.resolving.CWPropertyFileNameProcessor"
                .equals(projectDescriptor.getPropertiesFileNameProcessor())) {
                globalWarnMessages.add(
                    "CWPropertyFileNameProcessor is deprecated. 'CW' keyword support for 'state' property is moved to the default property processor. Remove declaration of this class from 'rules.xml'.");
            }
            Path rootProjectPath = projectDescriptor.getProjectFolder().toPath();
            for (Module module : projectDescriptor.getModules()) {
                Set<String> moduleErrorMessages = new HashSet<>(globalErrorMessages);
                Set<String> moduleWarnMessages = new HashSet<>(globalWarnMessages);
                Map<String, Object> params = new HashMap<>();
                if (processor != null) {
                    try {
                        Path rulesFullPath = Paths.get(module.getRulesRootPath().getPath());

                        String relativePath = rootProjectPath.relativize(rulesFullPath).toString().replace("\\", "/");

                        ITableProperties tableProperties = processor.process(relativePath);
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
        } catch (ValidationException ex) {
            throw new ProjectResolvingException(
                "Project descriptor is wrong. Please, verify '" + PROJECT_DESCRIPTOR_FILE_NAME + "' file format.",
                ex);
        } catch (FileNotFoundException e) {
            throw new ProjectResolvingException(
                "Project descriptor is not found. File '" + PROJECT_DESCRIPTOR_FILE_NAME + "' is missed.",
                e);
        } catch (Exception e) {
            throw new ProjectResolvingException("Failed to read project descriptor.", e);
        } finally {
            propertiesFileNameProcessorBuilder.destroy();
        }
    }
}
