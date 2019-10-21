package org.openl.rules.project.resolving;

import java.net.URL;
import java.net.URLClassLoader;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.StringUtils;

public final class PropertiesFileNameProcessorBuilder {
    public PropertiesFileNameProcessorBuilder() {
    }

    PropertiesFileNameProcessor processor;

    public PropertiesFileNameProcessor build(
            ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException {
        if (processor != null) {
            throw new IllegalStateException("Processor has already built! Use a new builder!");
        }
        if (StringUtils.isNotBlank(projectDescriptor.getPropertiesFileNameProcessor())) {
            processor = buildCustomProcessor(projectDescriptor);
        } else {
            processor = buildDefaultProcessor(projectDescriptor);
        }
        return processor;
    }

    public PropertiesFileNameProcessor buildCustomProcessor(
            ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException {
        if (processor != null) {
            throw new IllegalStateException("Processor has already built! Use a new builder!");
        }
        ClassLoader classLoader = getCustomClassLoader(projectDescriptor);
        try {
            Class<?> clazz = classLoader.loadClass(projectDescriptor.getPropertiesFileNameProcessor());
            processor = (PropertiesFileNameProcessor) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            String message = "Properties file name processor class '" + projectDescriptor
                .getPropertiesFileNameProcessor() + "' is not found.";
            throw new InvalidFileNameProcessorException(message, e);
        } catch (Exception e) {
            String message = "Failed to instantiate default properties file name processor! Class should have default constructor and implement org.openl.rules.project.resolving.PropertiesFileNameProcessor interface!";
            throw new InvalidFileNameProcessorException(message, e);
        } catch (NoClassDefFoundError e) {
            String message = "Properties file name processor class '" + projectDescriptor
                .getPropertiesFileNameProcessor() + "' has not been load!";
            throw new InvalidFileNameProcessorException(message, e);
        }
        return processor;
    }

    public PropertiesFileNameProcessor buildDefaultProcessor(
            ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException {
        if (processor != null) {
            throw new IllegalStateException("Processor has already built! Use a new builder!");
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> clazz = classLoader
                .loadClass("org.openl.rules.project.resolving.DefaultPropertiesFileNameProcessor");
            return (PropertiesFileNameProcessor) clazz.newInstance();
        } catch (Exception e) {
            // Should not occur in normal situation.
            throw new InvalidFileNameProcessorException(e);
        }
    }

    public void destroy() {
        if (classLoader != null) {
            ClassLoaderUtils.close(classLoader);
        }
    }

    URLClassLoader classLoader;

    protected ClassLoader getCustomClassLoader(ProjectDescriptor projectDescriptor) {
        URL[] urls = projectDescriptor.getClassPathUrls();
        classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        return classLoader;
    }
}
