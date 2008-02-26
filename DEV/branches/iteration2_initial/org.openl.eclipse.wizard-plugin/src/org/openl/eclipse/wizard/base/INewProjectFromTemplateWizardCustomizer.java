/*
 * Created on 29.10.2004
 */
package org.openl.eclipse.wizard.base;

import java.util.Properties;

import org.eclipse.core.runtime.CoreException;

/**
 * @author smesh
 */
public interface INewProjectFromTemplateWizardCustomizer
{
  public String getAntBuildFileLocation();

  public void setAntBuildFileProperties(Properties properties);

  /************************* from IUtilBase */
  public String getString(String key);
  public String getString(String key, String defaultValue);
  public CoreException handleException(Throwable t);
  /************************* EOF from IUtilBase */
}
