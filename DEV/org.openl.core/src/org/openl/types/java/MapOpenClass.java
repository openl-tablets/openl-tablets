/*
 * Created on Jul 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.java;

import java.util.Iterator;
import java.util.Map;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MapOpenClass extends JavaOpenClass
{

	protected String name;

	protected IOpenClass dynamicFieldType = JavaOpenClass.STRING;

  /**
   * @param instanceClass
   * @param schema
   */
  public MapOpenClass(IOpenSchema schema, String name)
  {
    super(Map.class, schema);
    this.name = name;
  }
  
  

  /* (non-Javadoc)
   * @see org.openl.base.INamedThing#getName()
   */
  public String getName()
  {
    return name;
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#fields()
   */
  public Iterator fields()
  {
    // TODO Auto-generated method stub
    return super.fields();
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenClass#getField(java.lang.String)
   */
  public synchronized IOpenField getField(String name)
  {
    IOpenField f = super.getField(name);
    if (f == null)
    {
    	f = new MapOpenField(name, dynamicFieldType);
    	fieldMap().put(name, f);
    }
    return f;
  }
  
  static public interface DynamicFieldPolicy
  {
  	public IOpenField getOpenField(String name);
  }
  
  
  


	static public class MapOpenField extends AOpenField
	{
		
    /**
     * @param name
     * @param type
     */
    public MapOpenField(String name, IOpenClass type)
    {
      super(name, type);
    }

	    /* (non-Javadoc)
     * @see org.openl.types.IOpenField#get(java.lang.Object)
     */
    public Object get(Object target, IRuntimeEnv env)
    {
      return ((Map)target).get(this.name);
    }

    /* (non-Javadoc)
     * @see org.openl.types.IOpenField#set(java.lang.Object, java.lang.Object)
     */
    public void set(Object target, Object value, IRuntimeEnv env)
    {
    	((Map)target).put(this.name, value);

    }

}  
  

}
