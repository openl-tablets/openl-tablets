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
            throw new IllegalStateException("Processor has already built! Use a new builder.");
        }
        String propertiesFileNameProcessor = projectDescriptor.getPropertiesFileNameProcessor();
        if (StringUtils.isBlank(propertiesFileNameProcessor)) {
            processor = new DefaultPropertiesFileNameProcessor();
        } else if (CWPropertyFileNameProcessor.class.getName().equals(propertiesFileNameProcessor)) {
            processor = new CWPropertyFileNameProcessor();
        } else {
            processor = buildCustomProcessor(projectDescriptor);
        }
        return processor;
    }

    private PropertiesFileNameProcessor buildCustomProcessor(
            ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException {
        if (processor != null) {
            throw new IllegalStateException("Processor has already built! Use a new builder.");
        }
        ClassLoader classLoader = getCustomClassLoader(projectDescriptor);
        String propertiesFileNameProcessor = projectDescriptor.getPropertiesFileNameProcessor();
        try {
            Class<?> clazz = classLoader.loadClass(propertiesFileNameProcessor);
            processor = (PropertiesFileNameProcessor) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            String message = "Properties file name processor class '" + propertiesFileNameProcessor + "' is not found.";
            throw new InvalidFileNameProcessorException(message, e);
        } catch (Exception e) {
            String message = "Failed to instantiate default properties file name processor! Class should have default constructor and implement org.openl.rules.project.resolving.PropertiesFileNameProcessor interface.";
            throw new InvalidFileNameProcessorException(message, e);
        } catch (NoClassDefFoundError e) {
            String message = "Properties file name processor class '" + propertiesFileNameProcessor + "' has not been load.";
            throw new InvalidFileNameProcessorException(message, e);
        }
        return processor;
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
