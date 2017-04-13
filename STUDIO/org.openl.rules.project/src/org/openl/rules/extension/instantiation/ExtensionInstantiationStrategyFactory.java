package org.openl.rules.extension.instantiation;

import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.project.model.Extension;
import org.openl.rules.project.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExtensionInstantiationStrategyFactory {
    public static ExtensionInstantiationStrategy getInstantiationStrategy(Extension extension,
            Module moduleInfo,
            boolean executionMode,
            IDependencyManager dependencyManager,
            ClassLoader classLoader) {

        try {
            return new ExtensionInstantiationStrategy(moduleInfo,
                    executionMode,
                    dependencyManager,
                    classLoader,
                    extension);
        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(ExtensionInstantiationStrategyFactory.class);
            log.error(e.getMessage(), e);
            throw new OpenLRuntimeException(String.format("Failed to resolve instantiation strategy for extension \"%s\"",
                    extension.getName()));
        }
    }

}

