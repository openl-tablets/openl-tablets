package org.openl.rules.extension.instantiation;

import org.apache.commons.lang3.StringUtils;
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
        String openlName;
        try {
            openlName = extension.getExtensionPackage();
            if (StringUtils.isBlank(openlName)) {
                // Get OpenL name using convention
                openlName = "org.openl.extension." + StringUtils.lowerCase(extension.getName());
            }

            // TODO Check that OpenLBuilder for openlName exists, otherwise show meaningful message

            return new ExtensionInstantiationStrategy(moduleInfo,
                    executionMode,
                    dependencyManager,
                    classLoader,
                    openlName);
        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(ExtensionInstantiationStrategyFactory.class);
            log.error(e.getMessage(), e);
            throw new OpenLRuntimeException(String.format("Cannot resolve instantiation strategy for extension \"%s\"",
                    extension.getName()));
        }
    }

}

