/*
 * Created on 29.10.2004
 */
package org.openl.eclipse.wizard.base;

import java.util.Properties;

import org.osgi.framework.Bundle;

/**
 * @author smesh
 */
public class NewProjectFromTemplateWizardCustomizer extends UtilBase implements
        INewProjectFromTemplateWizardCustomizer, INewProjectFromTemplateWizardCustomizerConstants {

    Bundle descriptor;

    String propertyKeyPrefix;

    public NewProjectFromTemplateWizardCustomizer(Bundle descriptor, String propertyKeyPrefix) {
        this.descriptor = descriptor;
        this.propertyKeyPrefix = propertyKeyPrefix;
    }

    public NewProjectFromTemplateWizardCustomizer(Bundle descriptor, String propertyKeyPrefix, String resourceBundleName) {
        this(descriptor, propertyKeyPrefix);
        setResourceBundleName(resourceBundleName);
    }

    public String getPropertyKeyPrefix() {
        return propertyKeyPrefix;
    }

    @Override
    public String getString(String key, String defaultValue) {
        return super.getString(getPropertyKeyPrefix() == null ? key : getPropertyKeyPrefix() + "." + key, defaultValue);
    }

    public String getTemplateProjectDir() {
        String templateProjectDir = getString(KEY_TEMPLATE_PROJECT_DIR);

        String result = toCanonicalUrl(descriptor, templateProjectDir);

        if (result == null) {
            throw new RuntimeException(
                    "Template project directory does not exist. OpenlWizardPlugin installation URL: <"
                            + toCanonicalUrl(descriptor, "") + ">. Template project sub-directory: <"
                            + templateProjectDir + ">");
        }

        return result;
    }

    public void setTemplateProperties(Properties properties) {
        properties.setProperty(PROP_SRC_DIR, getTemplateProjectDir());
    }

}
