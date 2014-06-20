package org.openl.rules.project.resolving;

import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.rules.project.model.ProjectDescriptor;

public final class PropertiesFileNameProcessorBuilder {
    private PropertiesFileNameProcessorBuilder() {
    }

    public static PropertiesFileNameProcessor build(ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException {
        PropertiesFileNameProcessor processor;
        if (!StringUtils.isBlank(projectDescriptor.getPropertiesFileNameProcessor())) {
            processor = buildCustomProcessor(projectDescriptor);
        } else {
            processor = buildDefaultProcessor(projectDescriptor);
        }
        return processor;
    }

    public static PropertiesFileNameProcessor buildCustomProcessor(ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException {
        ClassLoader classLoader = getClassLoader(projectDescriptor);
        PropertiesFileNameProcessor processor;
        try {
            Class<?> clazz = classLoader.loadClass(projectDescriptor.getPropertiesFileNameProcessor());
            processor = (PropertiesFileNameProcessor) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            String message = "Properties file name processor class '" + projectDescriptor.getPropertiesFileNameProcessor() + "' wasn't found!";
            throw new InvalidFileNameProcessorException(message, e);
        } catch (Exception e) {
            String message = "Failed to instantiate default properties file name processor! Class should have default constructor and implement org.openl.rules.project.resolving.PropertiesFileNameProcessor interface!";
            throw new InvalidFileNameProcessorException(message, e);
        }
        return processor;
    }

    public static PropertiesFileNameProcessor buildDefaultProcessor(ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException {
        ClassLoader classLoader = getClassLoader(projectDescriptor);
        try {
            Class<?> clazz = classLoader.loadClass("org.openl.rules.project.resolving.DefaultPropertiesFileNameProcessor");
            return (PropertiesFileNameProcessor) clazz.newInstance();
        } catch (Exception e) {
            // Should not occur in normal situation.
            throw new InvalidFileNameProcessorException(e);
        }
    }

    protected static ClassLoader getClassLoader(ProjectDescriptor projectDescriptor) {
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(ProjectDescriptorBasedResolvingStrategy.class.getClassLoader());
        URL[] urls = projectDescriptor.getClassPathUrls();
        classLoader.addClassLoader(projectDescriptor.getClassLoader(false));
        OpenLClassLoaderHelper.extendClasspath(classLoader, urls);
        return classLoader;
    }
}
