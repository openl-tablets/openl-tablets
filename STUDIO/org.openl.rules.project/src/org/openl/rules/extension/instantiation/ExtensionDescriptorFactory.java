package org.openl.rules.extension.instantiation;

import org.openl.rules.project.model.Extension;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExtensionDescriptorFactory {
    private ExtensionDescriptorFactory() {
    }

    public static IExtensionDescriptor getExtensionDescriptor(Extension extension, ClassLoader classLoader) {
        IExtensionDescriptor descriptor;
        try {
            String extensionPackage;
            extensionPackage = extension.getExtensionPackage();
            if (StringUtils.isBlank(extensionPackage)) {
                // Get extension package using convention
                extensionPackage = "org.openl.extension." + extension.getName().toLowerCase();
            }

            Class<?> extensionClass = classLoader.loadClass(extensionPackage + ".ExtensionDescriptor");

            descriptor = (IExtensionDescriptor) extensionClass.newInstance();
        } catch (ClassNotFoundException e) {
            Logger log = LoggerFactory.getLogger(ExtensionDescriptorFactory.class);
            log.error(e.getMessage(), e);
            throw new ExtensionRuntimeException(String.format("Extension '%s' does not exist.", extension.getName()));
        } catch (InstantiationException e) {
            Logger log = LoggerFactory.getLogger(ExtensionDescriptorFactory.class);
            log.error(e.getMessage(), e);
            throw new ExtensionRuntimeException(
                String.format("Failed to instantiate extension '%s'", extension.getName()));
        } catch (IllegalAccessException e) {
            Logger log = LoggerFactory.getLogger(ExtensionDescriptorFactory.class);
            log.error(e.getMessage(), e);
            throw new ExtensionRuntimeException(String.format("Extension '%s' is not accessible!", extension.getName()));
        }
        return descriptor;
    }
}
