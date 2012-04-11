/*
 * Created on Jun 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.java;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.binding.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.types.ITypeLibrary;

/**
 * @author snshor
 *
 */
public class JavaPrimitiveTypeLibrary implements ITypeLibrary
{

  /* (non-Javadoc)
   * @see org.openl.binding.ITypeLibrary#findType(java.lang.String)
   */
  public IOpenClass getType(String typename) throws AmbiguousTypeException
  {
  	return  (IOpenClass)classMap.get(typename);
  }
  
 
  
  static final Map classMap;  
  static 
  {
  	classMap = new HashMap();
  	classMap.put("int", JavaOpenClass.INT);
		classMap.put("long", JavaOpenClass.LONG);
		classMap.put("char", JavaOpenClass.CHAR);
		classMap.put("short", JavaOpenClass.SHORT);
		classMap.put("byte", JavaOpenClass.BYTE);
		classMap.put("double", JavaOpenClass.DOUBLE);
		classMap.put("float", JavaOpenClass.FLOAT);
		classMap.put("boolean", JavaOpenClass.BOOLEAN);
		classMap.put("void", JavaOpenClass.VOID);
  } 
  
  
  
  

  /* (non-Javadoc)
   * @see org.openl.binding.ITypeLibrary#types()
   */
  public Iterator typeNames()
  {
    return classMap.keySet().iterator();
  }

}
