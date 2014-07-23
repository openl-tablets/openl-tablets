package org.openl.rules.project.resolving;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.classloader.ClassLoaderCloserFactory;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.engine.OpenLSourceManager;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesLoader;

public class ProjectDescriptorBasedResolvingStrategy extends BaseResolvingStrategy {

    private final Log log = LogFactory.getLog(ProjectDescriptorBasedResolvingStrategy.class);

    public final static String PROJECT_DESCRIPTOR_FILE_NAME = "rules.xml";

    public boolean isRulesProject(File folder) {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        if (descriptorFile.exists()) {
            log.debug(String.format("Project in %s folder was resolved as Project descriptor based project",
                folder.getPath()));
            return true;
        } else {
            log.debug(String.format("Project descriptor based strategy failed to resolve project folder %s:" + "there is no file %s in folder",
                folder.getPath(),
                PROJECT_DESCRIPTOR_FILE_NAME));
            return false;
        }
    }

    private URLClassLoader classLoader;

    protected ClassLoader getClassLoader(ProjectDescriptor projectDescriptor) {
        URL[] urls = projectDescriptor.getClassPathUrls();
        classLoader = new URLClassLoader(urls, ProjectDescriptorBasedResolvingStrategy.class.getClassLoader());
        return classLoader;
    }

    protected ProjectDescriptor internalResolveProject(File folder) throws ProjectResolvingException {
        File descriptorFile = new File(folder, PROJECT_DESCRIPTOR_FILE_NAME);
        ProjectDescriptorManager descriptorManager = new ProjectDescriptorManager();
        Set<String> globalErrorMessages = new LinkedHashSet<String>();
        Set<String> globalWarnMessages = new LinkedHashSet<String>();
        PropertiesFileNameProcessor processor = null;
        try {
            ProjectDescriptor projectDescriptor = descriptorManager.readDescriptor(descriptorFile);
            processor = buildProcessor(globalErrorMessages, projectDescriptor);
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
                        if (log.isWarnEnabled()) {
                            log.warn("Module file name doesn't match to file name pattern!");
                            if (e.getMessage() != null) {
                                log.warn(e.getMessage());
                            }
                        }
                        if (e.getMessage() == null) {
                            moduleWarnMessages.add("Module file name doesn't match to file name pattern!");
                        } else {
                            if (!(processor instanceof DefaultPropertiesFileNameProcessor)) {
                                moduleWarnMessages.add("Module file name doesn't match to file name pattern! " + e.getMessage());
                            } else {
                                moduleWarnMessages.add(e.getMessage());
                            }
                        }
                    } catch (InvalidFileNamePatternException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("File name pattern is invalid!");
                            if (e.getMessage() != null) {
                                log.warn(e.getMessage());
                            }
                        }
                        if (e.getMessage() == null) {
                            moduleErrorMessages.add("File name pattern is invalid!");
                        } else {
                            if (!(processor instanceof DefaultPropertiesFileNameProcessor)) {
                                moduleErrorMessages.add("File name pattern is invalid! " + e.getMessage());
                            } else {
                                moduleErrorMessages.add(e.getMessage());
                            }
                        }
                    }
                    params.put(OpenLSourceManager.PROPERTIES_FILE_NAME_PATTERN_ERROR_MESSAGES_KEY, moduleErrorMessages);
                    params.put(OpenLSourceManager.PROPERTIES_FILE_NAME_PATTERN_WARN_MESSAGES_KEY, moduleWarnMessages);
                    module.setProperties(params);
                }
            }
            return projectDescriptor;
        } catch (ValidationException ex) {
            throw new ProjectResolvingException("Project descriptor is invalid. Please, verify " + PROJECT_DESCRIPTOR_FILE_NAME + " file format.",
                ex);
        } catch (FileNotFoundException e) {
            throw new ProjectResolvingException("Project descriptor wasn't found! Project should countain " + PROJECT_DESCRIPTOR_FILE_NAME + " file.",
                e);
        } catch (Exception e) {
            throw new ProjectResolvingException("Project descriptor reading failed.", e);
        } finally {
            destroy();
        }
    }

    private PropertiesFileNameProcessor buildProcessor(Set<String> globalErrorMessages,
            ProjectDescriptor projectDescriptor) throws ClassNotFoundException,
                                                InstantiationException,
                                                IllegalAccessException {
        PropertiesFileNameProcessor processor = null;
        if (projectDescriptor.getPropertiesFileNameProcessor() != null && !projectDescriptor.getPropertiesFileNameProcessor()
            .isEmpty()) {
            ClassLoader classLoader = getClassLoader(projectDescriptor);
            try {
                Class<?> clazz = classLoader.loadClass(projectDescriptor.getPropertiesFileNameProcessor());
                processor = (PropertiesFileNameProcessor) clazz.newInstance();
            } catch (ClassNotFoundException e) {
                String message = "Properties file name processor class '" + projectDescriptor.getPropertiesFileNameProcessor() + "' wasn't found!";
                if (log.isWarnEnabled()) {
                    log.warn(message);
                }
                globalErrorMessages.add("Properties file name processor class '" + projectDescriptor.getPropertiesFileNameProcessor() + "' wasn't found!");
            } catch (Exception e) {
                String message = "Failed to instantiate default properties file name processor! Class should have default constructor and implement org.openl.rules.project.resolving.PropertiesFileNameProcessor interface!";
                if (log.isWarnEnabled()) {
                    log.warn(message);
                }
                globalErrorMessages.add(message);
            }
        } else {
            if (projectDescriptor.getPropertiesFileNamePattern() != null && !projectDescriptor.getPropertiesFileNamePattern()
                .isEmpty()) {
                Class<?> clazz = ProjectDescriptorBasedResolvingStrategy.class.getClassLoader().loadClass("org.openl.rules.project.resolving.DefaultPropertiesFileNameProcessor");
                processor = (PropertiesFileNameProcessor) clazz.newInstance();
            }
        }
        return processor;
    }

    private void destroy() {
        if (classLoader != null) {
            ClassLoaderCloserFactory.getClassLoaderCloser().close(classLoader);
	    classLoader = null;
        }
    }
}
