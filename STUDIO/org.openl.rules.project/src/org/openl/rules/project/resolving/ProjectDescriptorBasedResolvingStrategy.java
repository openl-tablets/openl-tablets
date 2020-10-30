package org.openl.rules.project.resolving;

import java.io.File;
import java.io.FileNotFoundException;
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
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectDescriptorBasedResolvingStrategy implements ResolvingStrategy {

    public static final String PROJECT_DESCRIPTOR_FILE_NAME = "rules.xml";
    private static final Logger LOG = LoggerFactory.getLogger(ProjectDescriptorBasedResolvingStrategy.class);

    @Override
    public boolean isRulesProject(File folder) {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        if (descriptorFile.exists()) {
            LOG.debug("Project in {} folder has been resolved as Project descriptor based project.", folder.getPath());
            return true;
        } else {
            LOG.debug(
                "Project descriptor based strategy has failed to resolve project folder {}: there is no file {} in folder.",
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
            if (StringUtils.isNotBlank(projectDescriptor.getPropertiesFileNameProcessor())) {
                try {
                    processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
                } catch (InvalidFileNameProcessorException e1) {
                    String message = e1.getMessage();
                    LOG.warn(message);
                    globalErrorMessages.add(message);
                }
            } else {
                if (CollectionUtils.isNotEmpty(projectDescriptor.getPropertiesFileNamePatterns())) {
                    processor = propertiesFileNameProcessorBuilder.build(projectDescriptor);
                }
            }
            if (processor != null) {
                Set<String> globalWarnMessages = new LinkedHashSet<>();
                if (processor instanceof CWPropertyFileNameProcessor) {
                    globalWarnMessages.add("CWPropertyFileNameProcessor is deprecated. 'CW' keyword support for 'state' property was moved to the default property processor. Delete declaration of this class from rules.xml");
                }
                for (Module module : projectDescriptor.getModules()) {
                    Set<String> moduleErrorMessages = new HashSet<>(globalErrorMessages);
                    Set<String> moduleWarnMessages = new HashSet<>(globalWarnMessages);
                    Map<String, Object> params = new HashMap<>();
                    try {
                        ITableProperties tableProperties = processor.process(module,
                            projectDescriptor.getPropertiesFileNamePatterns());
                        params.put(PropertiesLoader.EXTERNAL_MODULE_PROPERTIES_KEY, tableProperties);
                    } catch (NoMatchFileNameException e) {
                        if (processor instanceof DefaultPropertiesFileNameProcessor) {
                            moduleWarnMessages.add(e.getMessage());
                        } else {
                            moduleWarnMessages.add("Module file name '" + module
                                .getName() + "' does not match to file name pattern! " + e.getMessage());
                        }
                    } catch (InvalidFileNamePatternException e) {
                        moduleErrorMessages.add(e.getMessage());
                    } catch (Exception | LinkageError e) {
                        LOG.warn("Failed to load custom file name processor.", e);
                        moduleErrorMessages.add("Failed to load custom file name processor.");
                    }
                    params.put(OpenLCompileManager.ADDITIONAL_ERROR_MESSAGES_KEY, moduleErrorMessages);
                    params.put(OpenLCompileManager.ADDITIONAL_WARN_MESSAGES_KEY, moduleWarnMessages);
                    module.setProperties(params);
                }
            }
            return projectDescriptor;
        } catch (ValidationException ex) {
            throw new ProjectResolvingException(
                "Project descriptor is wrong. Please, verify '" + PROJECT_DESCRIPTOR_FILE_NAME + "' file format.",
                ex);
        } catch (FileNotFoundException e) {
            throw new ProjectResolvingException(
                "Project descriptor is not found! Project must countain '" + PROJECT_DESCRIPTOR_FILE_NAME + "' file.",
                e);
        } catch (Exception e) {
            throw new ProjectResolvingException("Failed to read project descriptor.", e);
        } finally {
            propertiesFileNameProcessorBuilder.destroy();
        }
    }
}
