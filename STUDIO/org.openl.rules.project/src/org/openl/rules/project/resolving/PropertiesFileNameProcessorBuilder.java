package org.openl.rules.project.resolving;

import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.rules.project.model.ProjectDescriptor;

public class PropertiesFileNameProcessorBuilder {

    public PropertiesFileNameProcessor build(ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException {
        ClassLoader classLoader = getClassLoader(projectDescriptor);
        PropertiesFileNameProcessor processor;
        if (!StringUtils.isBlank(projectDescriptor.getPropertiesFileNameProcessor())) {
            processor = buildCustomProcessor(projectDescriptor, classLoader);
        } else {
            processor = buildDefaultProcessor(projectDescriptor, classLoader);
        }
        return processor;
    }

    protected PropertiesFileNameProcessor buildCustomProcessor(ProjectDescriptor projectDescriptor, ClassLoader classLoader) throws InvalidFileNameProcessorException {
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

    protected PropertiesFileNameProcessor buildDefaultProcessor(ProjectDescriptor projectDescriptor, ClassLoader classLoader) throws InvalidFileNameProcessorException {
        try {
            Class<?> clazz = classLoader.loadClass("org.openl.rules.project.resolving.DefaultPropertiesFileNameProcessor");
            return  (PropertiesFileNameProcessor) clazz.newInstance();
        } catch (Exception e) {
            // Should not occur in normal situation.
            throw new InvalidFileNameProcessorException(e);
        }
    }

    protected ClassLoader getClassLoader(ProjectDescriptor projectDescriptor) {
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(ProjectDescriptorBasedResolvingStrategy.class.getClassLoader());
        URL[] urls = projectDescriptor.getClassPathUrls();
        classLoader.addClassLoader(projectDescriptor.getClassLoader(false));
        OpenLClassLoaderHelper.extendClasspath(classLoader, urls);
        return classLoader;
    }
}
