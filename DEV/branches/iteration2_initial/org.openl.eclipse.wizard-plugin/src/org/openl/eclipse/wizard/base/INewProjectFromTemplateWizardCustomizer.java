/*
 * Created on 29.10.2004
 */
package org.openl.eclipse.wizard.base;

import java.util.Properties;

import org.openl.eclipse.wizard.base.internal.TemplateCopier;

/**
 * @author smesh
 */
public interface INewProjectFromTemplateWizardCustomizer
{
    /**
     * @deprecated ant build file is no longer used.
     * @see TemplateCopier
     */
  public String getAntBuildFileLocation();

  public void setAntBuildFileProperties(Properties properties);

  /************************* from IUtilBase */
  public String getString(String key);
  public String getString(String key, String defaultValue);
  /************************* EOF from IUtilBase */
}
