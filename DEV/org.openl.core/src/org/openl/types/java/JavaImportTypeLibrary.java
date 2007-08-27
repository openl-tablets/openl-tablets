/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.java;

import java.util.HashMap;
import java.util.Iterator;

import org.openl.binding.AmbiguousTypeException;
import org.openl.conf.ClassFactory;
import org.openl.types.IOpenClass;
import org.openl.types.ITypeLibrary;

/**
 * @author snshor
 *
 */
public class JavaImportTypeLibrary implements ITypeLibrary
{
	
	HashMap aliases = new HashMap();

	String[] importPackages;
	
	ClassLoader loader;


	public JavaImportTypeLibrary(String[] importClasses, String[] importPackages, ClassLoader loader)
	{
		this.loader = loader;
		this.importPackages = importPackages;
		if (importClasses != null)
		{
			for (int i = 0; i < importClasses.length; i++)
      {
      	int index = importClasses[i].lastIndexOf('.');
      	String alias = importClasses[i].substring(index + 1);
      	
      	Class c = ClassFactory.forName(importClasses[i], loader);
      	aliases.put(alias, JavaOpenClass.getOpenClass(c) );
      	      
      }
		}	
	}

	

  /* (non-Javadoc)
   * @see org.openl.types.ITypeLibrary#getType(java.lang.String)
   */
  public synchronized IOpenClass getType(String typename) throws AmbiguousTypeException
  {
  	
  	
  	IOpenClass oc = (IOpenClass)aliases.get(typename);
  	if (oc != null)
    	return oc;
		//TODO use imports  	
    for (int i = 0; i < importPackages.length; i++)
    {
    	try
    	{
    		Class c = ClassFactory.forName(importPackages[i] + "." + typename, getClassLoader());
    		oc = JavaOpenClass.getOpenClass(c);
    		aliases.put(typename, oc);
    		return oc;
    	}
			catch(Throwable t)
			{
			}      
    }
    
    return null;
		    
  }
  
  protected ClassLoader getClassLoader()
  {
  	return loader;
  }

  /* (non-Javadoc)
   * @see org.openl.types.ITypeLibrary#types()
   */
  public Iterator typeNames()
  {
    // TODO Auto-generated method stub
    return null;
  }

}
