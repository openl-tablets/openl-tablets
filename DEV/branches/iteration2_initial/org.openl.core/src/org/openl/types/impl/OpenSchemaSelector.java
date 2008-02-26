/*
 * Created on Aug 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.impl;

import java.util.Iterator;

import org.openl.binding.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenSchema;
import org.openl.util.ISelector;
import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public class OpenSchemaSelector extends OpenSchemaDelegator
{

	ISelector selector;

  /**
   * @param delegate
   */
  public OpenSchemaSelector(IOpenSchema delegate)
  {
    super(delegate);
  }
  
  

  /* (non-Javadoc)
   * @see org.openl.types.ITypeLibrary#getType(java.lang.String)
   */
  public IOpenClass getType(String typename) throws AmbiguousTypeException
  {
  	if (!selector.select(typename))
  	  return null;
    return super.getType(typename);
  }

  /* (non-Javadoc)
   * @see org.openl.types.ITypeLibrary#types()
   */
  public Iterator typeNames()
  {
    return  OpenIterator.select(super.typeNames(), selector);
  }

}
