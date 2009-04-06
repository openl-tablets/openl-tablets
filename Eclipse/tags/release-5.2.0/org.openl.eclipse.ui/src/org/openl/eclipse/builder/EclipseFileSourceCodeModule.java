/*
 * Created on Oct 9, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.builder;

import org.eclipse.core.resources.IFile;
import org.openl.syntax.impl.FileSourceCodeModule;

/**
 * 
 * @author sam
 */
public class EclipseFileSourceCodeModule extends FileSourceCodeModule
{

  public EclipseFileSourceCodeModule(IFile file)
  {
    super(file.getLocation().toString(), getUri(file), getTabSize(file));
  }
  
  
  static String getUri(IFile file)
  {
	  
	  return "file:" + file.getLocation().toString();
  }
  
  //TODO get from Eclipse preferences
  static int getTabSize(IFile file)
  {
	  return 2;
  }
  
}
