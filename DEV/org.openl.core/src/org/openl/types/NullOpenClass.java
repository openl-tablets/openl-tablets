/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types;

import java.util.Iterator;

import org.openl.binding.AmbiguousMethodException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class NullOpenClass implements IOpenClass
{
	
	
	private NullOpenClass(){}
	
	public static final NullOpenClass the = new NullOpenClass();

  public IOpenSchema getSchema()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public IOpenFactory getFactory()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public Iterator superClasses()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public Iterator methods()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#fields()
   */
  public Iterator fields()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#isAbstract()
   */
  public boolean isAbstract()
  {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isInstance(Object instance)
  {
    return instance == null;
  }

  public boolean isAssignableFrom(Class c)
  {
    return false;
  }

  public Class getInstanceClass()
  {
    return null;
  }

  public IOpenMethod getMethod(String name, IOpenClass[] classes)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#getField(java.lang.String)
   */
  public IOpenField getField(String name)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see org.openl.base.INamedThing#getName()
   */
  public String getName()
  {
    return "null-Class";
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#isAssignableFrom(org.openl.types.IOpenClass)
   */
  public boolean isAssignableFrom(IOpenClass ioc)
  {
    return ioc == this;
  }
  
  public Object nullObject()
  {
  	return null;
  }
  
  
  

  /* (non-Javadoc)
   * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String, org.openl.types.IOpenClass[])
   */
  public IOpenMethod getMatchingMethod(String name, IOpenClass[] params)
    throws AmbiguousMethodException
  {
    return null;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IVarFactory#getVar(java.lang.String)
   */
  public IOpenField getVar(String name)
  {
    return null;
  }




  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#isArray()
   */
  public boolean isArray()
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#getArrayType(int)
   */
  public IOpenClass getArrayType(int dim)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#getAggregateInfo()
   */
  public IAggregateInfo getAggregateInfo()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClassHolder#getOpenClass()
   */
  public IOpenClass getOpenClass()
  {
    return this;
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#newInstance()
   */
  public Object newInstance(IRuntimeEnv env)
  {
    return null;
  }

	public IOpenField getIndexField()
	{
		return null;
	}

	public IMetaInfo getMetaInfo()
	{
		return null;
	}

	public void setMetaInfo(IMetaInfo info)
	{
	}

	public String getDisplayName(int mode)
	{
		return getName();
	}

	public boolean isAssignableFrom(IType type) {
		return false;
	}

	public String getNameSpace() {
		return ISyntaxConstants.THIS_NAMESPACE;
	}

	public IDomain getDomain() {
		return null;
	}

}
