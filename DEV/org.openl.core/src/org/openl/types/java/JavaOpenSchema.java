/*
 * Created on Jul 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenClassHolder;
import org.openl.types.IOpenFactory;
import org.openl.types.impl.AOpenSchema;
import org.openl.util.ASelector;
import org.openl.util.FileTreeIterator;
import org.openl.util.IConvertor;
import org.openl.util.ISelector;
import org.openl.util.OpenIterator;
import org.openl.util.RuntimeExceptionWrapper;


/**
 * @author snshor
 *
 */
public class JavaOpenSchema extends AOpenSchema
{

  String[] classpath;
  
	ClassLoader classLoader;  

  /**
   * @param factory
   */
  public JavaOpenSchema(IOpenFactory factory, String[] classpath, ClassLoader classLoader)
  {
    super(factory);
		this.classpath = classpath;
		this.classLoader = classLoader;
		
  }

  /**
   * @return
   */
  public String[] getClasspath()
  {
    return classpath;
  }

  /**
   * @param string
   */
  public void setClasspath(String[] string)
  {
    classpath = string;
  }

  /* (non-Javadoc)
   * @see org.openl.types.impl.AOpenSchema#buildAllClasses()
   */
  protected Map buildAllClasses()
  {
  	HashMap map = new HashMap();
  	
  	for (int i = 0; i < classpath.length; i++)
    {
    	try
    	{
				for (Iterator iter = getIterator(classpath[i]); iter.hasNext();)
				{
					String className = (String)iter.next();
					map.put(className, new JavaOpenClassHolder(className, classLoader));        	
				}
    	}
    	catch(Exception ex)
    	{
    		RuntimeExceptionWrapper.wrap(ex);
    	}
    }
  	
    return map;
  }
  
  static class JavaOpenClassHolder implements IOpenClassHolder
  {
  	ClassLoader classLoader;
  	String className;
  	IOpenClass javaOpenClass;
  	
		JavaOpenClassHolder(String className, ClassLoader classLoader)
  	{
  		this.className = className;
  		this.classLoader = classLoader;
  	}
  	
  	
  	
      /* (non-Javadoc)
     * @see org.openl.types.IOpenClassHolder#getOpenClass()
     */
    public IOpenClass getOpenClass()
    {
    	try
    	{
				if (javaOpenClass == null)
					javaOpenClass = JavaOpenClass.getOpenClass(classLoader.loadClass(className));
				return javaOpenClass;
    	}
    	catch(Exception ex)
    	{
    		throw RuntimeExceptionWrapper.wrap(ex);
    	}
    }

    /* (non-Javadoc)
     * @see org.openl.base.INamedThing#getName()
     */
    public String getName()
    {
      return className;
    }



		/* (non-Javadoc)
		 * @see org.openl.base.INamedThing#getDisplayName(int)
		 */
		public String getDisplayName(int mode)
		{
			return javaOpenClass.getDisplayName(mode);
		}

}
  

  protected Iterator getIterator(String classPathComponent)
    throws Exception
  {
    //determine a type of classpath component
    //it will be either .jar, .zip, .war files or directory
    //TODO if this is URL not a file we need to have a mechanism to deal with it
    
    
    if (classPathComponent.endsWith(".jar") || classPathComponent.endsWith(".zip"))
    	return getJarOrZipIterator(classPathComponent);
    else if (classPathComponent.endsWith(".war"))
      //TODO .war support
      throw	new UnsupportedOperationException(".war archives are not supported yet");
    else 
      return getDirectoryIterator(classPathComponent);    
     
  }

  protected Iterator getJarOrZipIterator(String jarname) throws Exception
  {
    ZipFile zip = new ZipFile(jarname);

		IConvertor zipToStringCollector = new IConvertor()
		{
			public Object convert(Object obj)
			{
				return ((ZipEntry)obj).getName();
			}
		};
		

		return OpenIterator.fromEnumeration(zip.entries())
			.collect(zipToStringCollector)
			.select(CLASSFILENAME_SELECTOR)
			.collect(new FileNameToClassCollector(0, File.separatorChar));
      
      }

  protected Iterator getDirectoryIterator(String dirname) throws Exception
  {


    IConvertor fileToStringCollector = new IConvertor()
    {

      public Object convert(Object obj)
      {
        File f = (File)obj;
        return f.getAbsolutePath();
      }
    };

    File dir = new File(dirname).getCanonicalFile();

    String dirName = dir.getAbsolutePath();

    return new FileTreeIterator(dir, 0)
      .collect(fileToStringCollector)
		  .select(CLASSFILENAME_SELECTOR)
      .collect(new FileNameToClassCollector(dirName.length() + 1, File.separatorChar));
  }

  static class ClassNameSelector extends ASelector
  {

    public boolean select(Object obj)
    {
      return ((String)obj).endsWith(".class");
    }

  }
  
	static final ISelector CLASSFILENAME_SELECTOR = new ClassNameSelector();
  

  static class FileNameToClassCollector implements IConvertor
  {
    int rootlength;
    char separator;

    FileNameToClassCollector(int rootlength, char separator)
    {
      this.rootlength = rootlength; // adjust for last "/" or "\"
      this.separator = separator;
    }

    /* (non-Javadoc)
     * @see org.openl.util.ICollector#collect(java.lang.Object)
     */
    public Object convert(Object obj)
    {
      String s = (String)obj;
      s = s.substring(rootlength, s.length() - 6); //ends with ".class"

      int len = s.length();
      StringBuffer buf = new StringBuffer(len);

      for (int i = 0; i < len; i++)
      {
        char c = s.charAt(i);
        if (c == separator)
          buf.append('.');
        else
          buf.append(c);
      }

      return buf.toString();

    }

  }
}
