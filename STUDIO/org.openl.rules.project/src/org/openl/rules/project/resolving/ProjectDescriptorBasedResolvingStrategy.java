package org.openl.rules.project.resolving;

import org.openl.engine.OpenLSourceManager;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ProjectDescriptorBasedResolvingStrategy implements ResolvingStrategy {

    private final Logger log = LoggerFactory.getLogger(ProjectDescriptorBasedResolvingStrategy.class);

    public final static String PROJECT_DESCRIPTOR_FILE_NAME = "rules.xml";

    public boolean isRulesProject(File folder) {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        if (descriptorFile.exists()) {
            log.debug("Project in {} folder has been resolved as Project descriptor based project.", folder.getPath());
            return true;
        } else {
            log.debug("Project descriptor based strategy has failed to resolve project folder {}: there is no file {} in folder.", folder.getPath(), PROJECT_DESCRIPTOR_FILE_NAME);
            return false;
        }
    }

    public ProjectDescriptor resolveProject(File folder) throws ProjectResolvingException {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        ProjectDescriptorManager descriptorManager = new ProjectDescriptorManager();
        Set<String> globalErrorMessages = new LinkedHashSet<String>();
        Set<String> globalWarnMessages = new LinkedHashSet<String>();
        PropertiesFileNameProcessor processor = null;
        PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder = new PropertiesFileNameProcessorBuilder();
        try {
            ProjectDescriptor projectDescriptor = descriptorManager.readDescriptor(descriptorFile);
            processor = buildProcessor(globalErrorMessages, projectDescriptor, propertiesFileNameProcessorBuilder);
            if (processor != null) {
                for (Module module : projectDescriptor.getModules()) {
                    Set<String> moduleErrorMessages = new HashSet<String>(globalErrorMessages);
                    Set<String> moduleWarnMessages = new HashSet<String>(globalWarnMessages);
                    Map<String, Object> params = new HashMap<String, Object>();
                    try {
                        ITableProperties tableProperties = processor.process(module,
                                projectDescriptor.getPropertiesFileNamePattern());
                        params.put(PropertiesLoader.EXTERNAL_MODULE_PROPERTIES_KEY, tableProperties);
                    } catch (NoMatchFileNameException e) {
                        String moduleFileName = FilenameExtractorUtil.extractFileNameFromModule(module);
                        String defaultMessage = null;
                        if (projectDescriptor.getPropertiesFileNamePattern() != null){
                            defaultMessage = "Module file name '" + moduleFileName + "' doesn't match file name pattern! File name pattern is: " + projectDescriptor.getPropertiesFileNamePattern();
                        }else{
                            defaultMessage = "Module file name '" + moduleFileName + "' doesn't match file name pattern!";
                        }

                        if (log.isWarnEnabled()){
                            if (e.getMessage() != null) {
                                log.warn(e.getMessage());
                            }else{
                                log.warn(defaultMessage);
                            }
                        }
                        if (e.getMessage() == null) {
                            moduleWarnMessages.add(defaultMessage);
                        } else {
                            if (!(processor instanceof DefaultPropertiesFileNameProcessor)) {
                                moduleWarnMessages.add("Module file name '" + moduleFileName + "' doesn't match to file name pattern! " + e.getMessage());
                            } else {
                                moduleWarnMessages.add(e.getMessage());
                            }
                        }
                    } catch (InvalidFileNamePatternException e) {
                        log.warn("File name pattern is invalid!");
                        if (e.getMessage() != null) {
                            log.warn(e.getMessage());
                        }
                        if (e.getMessage() == null) {
                            moduleErrorMessages.add("Wrong file name pattern!");
                        } else {
                            if (!(processor instanceof DefaultPropertiesFileNameProcessor)) {
                                moduleErrorMessages.add("Wrong file name pattern! " + e.getMessage());
                            } else {
                                moduleErrorMessages.add(e.getMessage());
                            }
                        }
                    } catch (Exception | LinkageError e) {
                        log.warn("Failed to load custom file name processor!", e);
                        moduleErrorMessages.add("Failed to load custom file name processor!");
                    }
                    params.put(OpenLSourceManager.ADDITIONAL_ERROR_MESSAGES_KEY, moduleErrorMessages);
                    params.put(OpenLSourceManager.ADDITIONAL_WARN_MESSAGES_KEY, moduleWarnMessages);
                    module.setProperties(params);
                }
            }
            return projectDescriptor;
        } catch (ValidationException ex) {
            throw new ProjectResolvingException("Project descriptor is wrong. Please, verify '" + PROJECT_DESCRIPTOR_FILE_NAME + "' file format.",
                    ex);
        } catch (FileNotFoundException e) {
            throw new ProjectResolvingException("Project descriptor hasn't been found! Project must countain '" + PROJECT_DESCRIPTOR_FILE_NAME + "' file.",
                    e);
        } catch (Exception e) {
            throw new ProjectResolvingException("Failed to read project descriptor!", e);
        } finally {
            propertiesFileNameProcessorBuilder.destroy();
        }
    }

    private PropertiesFileNameProcessor buildProcessor(final Set<String> globalErrorMessages,
                                                       ProjectDescriptor projectDescriptor, PropertiesFileNameProcessorBuilder propertiesFileNameProcessorBuilder) throws InvalidFileNameProcessorException {
        if (StringUtils.isNotBlank(projectDescriptor.getPropertiesFileNameProcessor())) {
            try {
                return propertiesFileNameProcessorBuilder.buildCustomProcessor(projectDescriptor);
            } catch (InvalidFileNameProcessorException e) {
                String message = e.getMessage();
                log.warn(message);
                globalErrorMessages.add(message);
                return null;
            }
        } else {
            if (StringUtils.isNotBlank(projectDescriptor.getPropertiesFileNamePattern())) {
                return propertiesFileNameProcessorBuilder.buildDefaultProcessor(projectDescriptor);
            }

            return null;
        }
    }
}
