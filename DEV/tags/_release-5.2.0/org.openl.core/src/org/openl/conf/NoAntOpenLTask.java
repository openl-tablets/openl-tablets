package org.openl.conf;

import org.openl.OpenConfigurationException;
import org.openl.syntax.IGrammar;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Allows Java based configuration without Ant
 * @author snshor
 *
 */

public class NoAntOpenLTask
{
	
	boolean inheritExtendedConfigurationLoader = false;
	
	
	
	String uri = "java://source_code";
	boolean shared = false;
	
	static public IOpenLConfiguration lastConfiguration;

  OpenLConfiguration conf = new OpenLConfiguration();

  String category;
	String classpath;

  String extendsCategory;


	static public IOpenLConfiguration retrieveConfiguration()
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
  public void execute(IUserContext ucxt, String baseDir) 
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
      
      
//        ClassLoaderFactory.getOpenlCoreLoader();
      
      IOpenLConfiguration existing;
      if ((existing = OpenLConfiguration.getInstance(category, ucxt))
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
          OpenLConfiguration.getInstance(extendsCategory, ucxt))
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
      
      IConfigurableResourceContext cxt = getConfigurationContext(extendsConfiguration, ucxt, baseDir);
      
      conf.setParent(extendsConfiguration);
      conf.setConfigurationContext(cxt);
      conf.validate(cxt);
      OpenLConfiguration.register(category, ucxt, conf, shared);
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
    	throw RuntimeExceptionWrapper.wrap(e);
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

  IConfigurableResourceContext getConfigurationContext(IOpenLConfiguration extendsConfiguration, IUserContext ucxt, String baseDir)
    throws Exception
  {
  	ClassLoader parentLoader =  extendsConfiguration == null ? ClassLoaderFactory.getOpenlCoreLoader(null): extendsConfiguration.getConfigurationContext().getClassLoader();

		ClassLoader myClassLoader = parentLoader;
		if (classpath != null && classpath.trim().length() != 0)
		{
			//String baseDir = getProject().getBaseDir().getCanonicalPath();
			UserContext ucxt2 = new UserContext(null, baseDir);
				
			myClassLoader = 
			  ClassLoaderFactory.createUserClassloader(category, classpath, parentLoader, ucxt2);
		}
		else 
  	{
			if (!inheritExtendedConfigurationLoader)
				myClassLoader = ucxt.getUserClassLoader();
  	}
  	
    return new ConfigurableResourceContext(myClassLoader, conf);
  }

  public String getUri()
  {
    return uri;
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

	public void setUri(String uri)
	{
		this.uri = uri;
	}

	public boolean isInheritExtendedConfigurationLoader()
	{
		return inheritExtendedConfigurationLoader;
	}

	public void setInheritExtendedConfigurationLoader(
			boolean inheritExtendedConfigurationLoader)
	{
		this.inheritExtendedConfigurationLoader = inheritExtendedConfigurationLoader;
	}


	
	
}
