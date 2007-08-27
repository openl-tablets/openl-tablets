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

  /* (non-Javadoc)
   * @see org.openl.binding.IVarFactory#getVar(java.lang.String, java.lang.String)
   */
  public IOpenField getVar(String name)
  {
    return openClass.getField(name);
  }
  
  

  /* (non-Javadoc)
   * @see org.openl.binding.IMethodFactory#methods()
   */
  public Iterator methods()
  {
  	ISelector sel = new ASelector()
  	{
  		public boolean select(Object obj)
  		{
  			return ((IOpenMethod)obj).isStatic();
  		}
  		
  		//TODO fix if necessary
  		public int redefinedHashCode(){return 0;}
  		public boolean equalsSelector(ASelector sel){return this == sel;}
  		
  		
  	};
  	
    return  OpenIterator.select(openClass.methods(), sel);
  }

}
