/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.Iterator;

import org.openl.binding.IOpenLibrary;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public class StaticClassLibrary  implements IOpenLibrary
{

  public StaticClassLibrary()
  {
  }

  public StaticClassLibrary(IOpenClass openClass)
  {
    this.openClass = openClass;
  }

  IOpenClass openClass = null;


  /* (non-Javadoc)
   * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String, java.lang.String, org.openl.types.IOpenClass[])
   */
  public IOpenMethod getMatchingMethod(String name, IOpenClass[] params)
  {
    return openClass.getMethod(name, params);

  }

  public void setOpenClass(IOpenClass c)
  {
    openClass = c;
  }

  public IOpenField getVar(String name, boolean strictMatch)
  {
    return openClass.getField(name, strictMatch);
  }
  
  

  public Iterator<IOpenMethod> methods()
  {
  	ISelector<IOpenMethod> sel = new ASelector<IOpenMethod>()
  	{
  		public boolean select(IOpenMethod m)
  		{
  			return m.isStatic();
  		}
  		
  		//TODO fix if necessary
  		public int redefinedHashCode(){return "static".hashCode();}
  		public boolean equalsSelector(ASelector<?> xsel){return this == (Object)xsel;}
  		
  		
  	};
  	
    return  OpenIterator.select(openClass.methods(), sel);
  }

}
