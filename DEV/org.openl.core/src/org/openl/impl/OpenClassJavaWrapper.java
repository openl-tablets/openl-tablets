/*
 * Created on Oct 26, 2005
 *
 */

package org.openl.impl;

import java.net.MalformedURLException;
import java.net.URL;

import org.openl.CompiledOpenClass;
import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.syntax.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.util.PropertiesLocator;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class OpenClassJavaWrapper
{
  CompiledOpenClass __compiledClass;
  IRuntimeEnv __env;
  
  
  public OpenClassJavaWrapper(CompiledOpenClass compiledClass, IRuntimeEnv env)
  {
    __compiledClass = compiledClass;
    __env = env;
  }
  
  public Object newInstance()
  {
    return getOpenClass().newInstance(__env);
  }
  
  
  public IOpenClass getOpenClass()
  {
    return __compiledClass.getOpenClass();
  }
  
  public IRuntimeEnv getEnv()
  {
    return __env;
  }
  
  ///factory methods
  
  
  static public OpenClassJavaWrapper createWrapper(String openlName, IUserContext ucxt, IOpenSourceCodeModule src)
  {
    OpenL openl = OpenL.getInstance(openlName, ucxt);
    
    CompiledOpenClass openClass = openl.compileModuleWithErrors(src);
   
    return new OpenClassJavaWrapper(openClass, openl.getVm().getRuntimeEnv());
  }
  
  static public OpenClassJavaWrapper createWrapper(String openlName, IUserContext ucxt, String srcFile)
  {
    OpenL openl = OpenL.getInstance(openlName, ucxt);
    
    IOpenSourceCodeModule src = null;
    
    String fileOrURL =
      PropertiesLocator.locateFileOrURL(srcFile, ucxt.getUserClassLoader(), new String[] {ucxt.getUserHome()});
    
    if (fileOrURL == null)
      throw new RuntimeException("File " + srcFile + " is not found");
      
    
    try
    {
      if (fileOrURL.indexOf(':') < 2) //file
        src = new FileSourceCodeModule(fileOrURL, null);
      else
        src = new URLSourceCodeModule(new URL(fileOrURL));
    } catch (MalformedURLException e)
    {
      throw RuntimeExceptionWrapper.wrap(e);
    }
    
    
    CompiledOpenClass openClass = openl.compileModuleWithErrors(src);
   
    return new OpenClassJavaWrapper(openClass, openl.getVm().getRuntimeEnv());
    
    
  }

	/**
	 * @return
	 */
	public CompiledOpenClass getCompiledClass()
	{
		return __compiledClass;
	}

	/**
	 * @return
	 */
	public IOpenClass getOpenClassWithErrors()
	{
		return __compiledClass.getOpenClassWithErrors();
	}
  
  
  
  
  
  
}
