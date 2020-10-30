package org.openl.rules.project.resolving;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

public final class PropertiesFileNameProcessorBuilder {
    public PropertiesFileNameProcessorBuilder() {
    }

    PropertiesFileNameProcessor processor;

    public PropertiesFileNameProcessor build(
            ProjectDescriptor projectDescriptor) throws InvalidFileNameProcessorException,
                                                 InvalidFileNamePatternException {
        if (processor != null) {
            throw new IllegalStateException("Processor has already built! Use a new builder.");
        }
        String[] patterns = projectDescriptor.getPropertiesFileNamePatterns();
        String prcClass = projectDescriptor.getPropertiesFileNameProcessor();

        if (StringUtils.isBlank(prcClass) || prcClass
            .equals("org.openl.rules.project.resolving.CWPropertyFileNameProcessor")) {
            processor = buildDefault(patterns);
        } else {
            ClassLoader classLoader = getCustomClassLoader(projectDescriptor);
            Class<PropertiesFileNameProcessor> clazz;
            try {
                clazz = (Class<PropertiesFileNameProcessor>) classLoader.loadClass(prcClass);
            } catch (ClassNotFoundException e) {
                String message = "Properties file name processor class '" + prcClass + "' is not found.";
                throw new InvalidFileNameProcessorException(message, e);
            } catch (NoClassDefFoundError e) {
                String message = "Properties file name processor class '" + prcClass + "' has not been load.";
                throw new InvalidFileNameProcessorException(message, e);
            }

            if (!PropertiesFileNameProcessor.class.isAssignableFrom(clazz)) {
                String message = "Properties file name processor class '" + prcClass + "' should implement org.openl.rules.project.resolving.PropertiesFileNameProcessor interface.";
                throw new InvalidFileNameProcessorException(message);
            }

            Constructor<PropertiesFileNameProcessor> declaredConstructor;
            try {
                declaredConstructor = clazz.getDeclaredConstructor(String.class);
                if (CollectionUtils.isEmpty(patterns)) {
                    this.processor = buildCustom(declaredConstructor, (String) null);
                } else {
                    this.processor = buildCustom(declaredConstructor, patterns);
                }
            } catch (NoSuchMethodException e) {
                try {
                    declaredConstructor = clazz.getDeclaredConstructor();
                    processor = newInstance(declaredConstructor);
                } catch (NoSuchMethodException e1) {
                    String message = "Properties file name processor class '" + prcClass + "' should have constructor with String argument or default constructor.";
                    throw new InvalidFileNameProcessorException(message, e);
                }
            }
        }
        return processor;
    }


    static PropertiesFileNameProcessor buildDefault(String... patterns) throws InvalidFileNamePatternException {
        if (CollectionUtils.isNotEmpty(patterns)) {
            PropertiesFileNameProcessor[] prcsrs = new PropertiesFileNameProcessor[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                prcsrs[i] = new DefaultPropertiesFileNameProcessor(patterns[i]);
            }

            return wrapProcessors(prcsrs);
        }
        return null;
    }

    private PropertiesFileNameProcessor buildCustom(Constructor<PropertiesFileNameProcessor> procConstructor,
            String... patterns) throws InvalidFileNamePatternException, InvalidFileNameProcessorException {
        PropertiesFileNameProcessor[] prcsrs = new PropertiesFileNameProcessor[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            prcsrs[i] = newInstance(procConstructor, patterns[i]);
        }

        return wrapProcessors(prcsrs);
    }

    private PropertiesFileNameProcessor newInstance(Constructor<PropertiesFileNameProcessor> procConstructor,
            Object... args) throws InvalidFileNamePatternException, InvalidFileNameProcessorException {
        PropertiesFileNameProcessor prc;
        try {
            prc = procConstructor.newInstance(args);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof InvalidFileNamePatternException) {
                throw (InvalidFileNamePatternException) targetException;
            }
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
            String message = "Failed to instantiate file name processor class '" + procConstructor + "'. Constructor should have InvalidFileNamePatternException declaration only.";
            throw new InvalidFileNameProcessorException(message, e);
        } catch (Exception e) {
            String message = "Failed to instantiate file name processor class '" + procConstructor + "'.";
            throw new InvalidFileNameProcessorException(message, e);
        }
        return prc;
    }

    private static PropertiesFileNameProcessor wrapProcessors(PropertiesFileNameProcessor[] prcsrs) {
        if (prcsrs.length == 1) {
            return prcsrs[0];
        } else {
            return modulePath -> {
                // choose the suitable pattern
                NoMatchFileNameException error = null;
                for (PropertiesFileNameProcessor prc : prcsrs) {
                    try {
                        return prc.process(modulePath);
                    } catch (NoMatchFileNameException e) {
                        if (error != null) {
                            e.addSuppressed(error);
                        }
                        error = e;
                    }
                }

                throw error;
            };
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
