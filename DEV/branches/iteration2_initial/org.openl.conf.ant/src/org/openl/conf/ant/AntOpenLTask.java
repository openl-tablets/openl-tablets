/*
 *  Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;
import org.openl.OpenConfigurationException;
import org.openl.conf.ClassFactory;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.conf.IOpenLConfiguration;
import org.openl.conf.LibraryFactoryConfiguration;
import org.openl.conf.NodeBinderFactoryConfiguration;
import org.openl.conf.OpenFactoryConfiguration;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.TypeCastFactory;
import org.openl.conf.TypeFactoryConfiguration;
import org.openl.conf.UserContext;
import org.openl.syntax.IGrammar;

/**
 * @author snshor
 *
 */
public class AntOpenLTask extends Task
{

	boolean shared = false;
	
	static public IOpenLConfiguration lastConfiguration;

  OpenLConfiguration conf = new OpenLConfiguration();

  String category;
	String classpath;

  String extendsCategory;


	static IOpenLConfiguration retrieveConfiguration()
	{
		if (lastConfiguration == null)
		  throw new NullPointerException();
		IOpenLConfiguration ret = lastConfiguration;
		lastConfiguration = null;
		return ret;  
	}

	void saveConfiguration(IOpenLConfiguration conf)
	{
		lastConfiguration = conf;
	}

  /* (non-Javadoc)
   * @see org.apache.tools.ant.Task#execute()
   */
  public void execute() throws BuildException
  {

    //  	try
    //  	{
    try
    {
      if (category == null)
      {
        throw new OpenConfigurationException(
          "The category must be set",
          getUri(),
          null);
      }
      
      
     //   ClassLoaderFactory.getOpenlCoreLoader();
      
      IOpenLConfiguration existing;
      if ((existing = OpenLConfiguration.getInstance(category, AntOpenLBuilder.userCxt.top()))
        != null)
      {
        //has been loaded and registered already
//        getProject().addReference(getCategory() + ".configuration", existing);
        saveConfiguration(existing);
        return;
      }
      
      IOpenLConfiguration extendsConfiguration = null;
      if (extendsCategory != null)
      {
        if ((extendsConfiguration =
          OpenLConfiguration.getInstance(extendsCategory, AntOpenLBuilder.userCxt.top()))
          == null)
        {
          throw new OpenConfigurationException(
            "The extended category "
              + extendsCategory
              + " must have been loaded first",
            getUri(),
            null);
        }
      }
      
      IConfigurableResourceContext cxt = getConfigurationContext(extendsConfiguration);
      
      conf.setParent(extendsConfiguration);
      conf.setConfigurationContext(cxt);
      conf.validate(cxt);
      OpenLConfiguration.register(category, AntOpenLBuilder.userCxt.top(), conf, shared);
      //  	}
      //  	catch(Throwable t)
      //  	{
      //  		throw new BuildException(t, getLocation());
      //  	}
      //getProject().addReference(getCategory() + ".configuration", conf);
	  	saveConfiguration(conf);
    }
    catch (Exception e)
    {
    	e.printStackTrace(System.err);
    	throw new BuildException(e);
    }
  }

  public NodeBinderFactoryConfiguration createBindings()
  {
    NodeBinderFactoryConfiguration nbf = new NodeBinderFactoryConfiguration();
    conf.setBinderFactory(nbf);
    return nbf;
  }

  public TypeCastFactory createTypecast()
  {
    TypeCastFactory tcf = new TypeCastFactory();
    conf.setTypeCastFactory(tcf);
    return tcf;
  }

  public void addConfiguredTypeFactory(OpenFactoryConfiguration of)
  {
    conf.addOpenFactory(of);
  }

  public LibraryFactoryConfiguration createLibraries()
  {
    LibraryFactoryConfiguration mf = new LibraryFactoryConfiguration();
    conf.setMethodFactory(mf);
    return mf;
  }

  public ClassFactory createGrammar()
  {
    ClassFactory cf = new ClassFactory();
    cf.setExtendsClassName(IGrammar.class.getName());
    conf.setGrammarFactory(cf);
    return cf;
  }

  public TypeFactoryConfiguration createTypes()
  {
    TypeFactoryConfiguration mf = new TypeFactoryConfiguration();
    conf.setTypeFactory(mf);
    return mf;
  }

  IConfigurableResourceContext getConfigurationContext(IOpenLConfiguration extendsConfiguration)
    throws Exception
  {
  	ClassLoader parentLoader =  extendsConfiguration == null ? ClassLoaderFactory.getOpenlCoreLoader(null): extendsConfiguration.getConfigurationContext().getClassLoader();

		ClassLoader myClassLoader = parentLoader;
		if (classpath != null && classpath.trim().length() != 0)
		{
			String baseDir = getProject().getBaseDir().getCanonicalPath();
			UserContext ucxt = new UserContext(null, baseDir);
				
			myClassLoader = 
			  ClassLoaderFactory.createUserClassloader(category, classpath, parentLoader, ucxt);
		}
		else 
  	{
  		myClassLoader = AntOpenLBuilder.userCxt.top().getUserClassLoader();
  	}
  	
    return new ConfigurableResourceContext(myClassLoader, conf);
  }

  public String getUri()
  {
    Location loc = getLocation();
    return loc == null ? null : loc.toString();
  } /**
       * @return
       */
  public String getCategory()
  {
    return category;
  } 
  /**
   * @param string
   */
  
  public void setCategory(String string)
  {
    category = string;
  } 

  /**
   * @return
   */
  public String getExtendsCategory()
  {
    return extendsCategory;
  } 

  /**
   * @param string
   */
  public void setExtendsCategory(String string)
  {
    extendsCategory = string;
  }

  /**
   * @param string
   */
  public void setClasspath(String string)
  {
    classpath = string;
  }

	/**
	 * @return
	 */
	public boolean isShared()
	{
		return shared;
	}

	/**
	 * @param b
	 */
	public void setShared(boolean b)
	{
		shared = b;
	}

}
