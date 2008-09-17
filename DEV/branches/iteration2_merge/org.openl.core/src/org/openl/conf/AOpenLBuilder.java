package org.openl.conf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Stack;

import org.openl.OpenConfigurationException;
import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.syntax.impl.Parser;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.SimpleVM;

public abstract class AOpenLBuilder implements IOpenLBuilder
{

  static public UserContextStack userCxt = new UserContextStack();
  
  
  boolean inheritExtendedConfigurationLoader = false;

  static class UserContextStack extends ThreadLocal<Stack<IUserContext>>
  {

    /**
     *  
     */

    protected Stack<IUserContext> initialValue()
    {
      return new Stack<IUserContext>();
    }

    public IUserContext top()
    {
      return stack().peek();
    }

    public IUserContext pop()
    {
      return stack().pop();
    }

    public void push(IUserContext ucxt)
    {
      stack().push(ucxt);
    }

    protected Stack<IUserContext> stack()
    {
      return get();
    }

  }

  IConfigurableResourceContext configurableResourceContext;

  IUserContext ucxt;

  public AOpenLBuilder()
  {
  }

  ClassLoader myClassLoader()
  {
    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    String myName = getClass().getName();
    try
		{
			oldClassLoader.loadClass(myName);
			return oldClassLoader;
		} catch (ClassNotFoundException e)
		{
			return getClass().getClassLoader();
		}
  	
  }
  
  
  public OpenL build(String openl) throws OpenConfigurationException
  {
    OpenL op = new OpenL();
    boolean changedClassLoader = false;
    ClassLoader oldClassLoader = null;

    try
    {
      userCxt.push(ucxt);

      ClassLoader myClassLoader = myClassLoader();

      oldClassLoader = Thread.currentThread().getContextClassLoader();

      if (oldClassLoader != myClassLoader)
      {
        Thread.currentThread().setContextClassLoader(myClassLoader);
        changedClassLoader = true;
      }

      UserContext mycxt = new UserContext(myClassLoader, ucxt.getUserHome());
      
      NoAntOpenLTask naot = getNoAntOpenLTask();
      
      naot.setInheritExtendedConfigurationLoader(inheritExtendedConfigurationLoader);
      if (inheritExtendedConfigurationLoader)
      	naot.execute(ucxt, ucxt.getUserHome());
      else	
      	naot.execute(mycxt, ucxt.getUserHome());
      
      //      OpenLConfiguration conf =
      //        (OpenLConfiguration)helper.getConfigurationObject(
      //          getAntProjectConfigurationVariable(openl));

      IOpenLConfiguration conf = NoAntOpenLTask.retrieveConfiguration();

      op.setParser(new Parser(conf));

      op.setBinder(new Binder(conf, conf, conf, conf, conf, op));
      op.setVm(new SimpleVM());
    } catch (Exception ex)
    {
      throw RuntimeExceptionWrapper.wrap(ex);
    } finally
    {
      if (changedClassLoader)
        Thread.currentThread().setContextClassLoader(oldClassLoader);
      userCxt.pop();
    }
    return op;
  }

  /**
   * @param openl
   */
  protected Properties getProperties(String openl)
  {
    URL url = configurableResourceContext.findClassPathResource(openl.replace(
        '.', '/')
        + '/' + openl + ".ant.properties");
    if (url == null)
      return null;
    InputStream is = null;
    try
    {
      is = url.openStream();
      Properties p = new Properties();
      p.load(is);
      return p;
    } catch (IOException e)
    {
      throw RuntimeExceptionWrapper.wrap(e);
    } finally
    {
      try
      {
        if (is != null)
          is.close();
      } catch (Throwable t)
      {
        Log.error("Error closing stream", t);
      }
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openl.conf.IOpenLBuilder#setConfigurableResourceContext(org.openl.conf.IConfigurableResourceContext)
   */
  public void setConfigurableResourceContext(IConfigurableResourceContext cxt,
      IUserContext ucxt)
  {
    this.configurableResourceContext = cxt;
    this.ucxt = ucxt;
  }
  
  public abstract NoAntOpenLTask getNoAntOpenLTask();

	public IConfigurableResourceContext getConfigurableResourceContext()
	{
		return configurableResourceContext;
	}
	
	public IUserContext getUserContext()
	{
		return ucxt;
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
