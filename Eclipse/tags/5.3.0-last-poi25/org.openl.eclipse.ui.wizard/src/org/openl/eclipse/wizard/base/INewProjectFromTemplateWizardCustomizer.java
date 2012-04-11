/*
 * Created on 29.10.2004
 */
package org.openl.eclipse.wizard.base;

import java.util.Properties;

/**
 * @author smesh
 */
public interface INewProjectFromTemplateWizardCustomizer {

    /** *********************** from IUtilBase */
    public String getString(String key);

    public String getString(String key, String defaultValue);
    /** *********************** EOF from IUtilBase */

    public void setTemplateProperties(Properties properties);
}
