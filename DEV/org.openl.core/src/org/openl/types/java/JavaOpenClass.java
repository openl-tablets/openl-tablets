/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.openl.base.INamedThing;
import org.openl.binding.AmbiguousMethodException;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.AOpenClass;
import org.openl.types.impl.ArrayIndex;
import org.openl.types.impl.ArrayLengthOpenField;
import org.openl.util.CollectionsUtil;
import org.openl.util.IConvertor;
import org.openl.util.IOpenIterator;
import org.openl.util.OpenIterator;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class JavaOpenClass extends AOpenClass
{
	protected Class instanceClass;

	protected HashMap fields = null;
	protected HashMap methods = null;

	protected JavaOpenClass(Class instanceClass, IOpenSchema schema)
	{
		super(schema);
		this.instanceClass = instanceClass;
		this.schema = schema;
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenClass#superClasses()
	 */
	public Iterator superClasses()
	{
		IOpenIterator interfaces =
			OpenIterator.fromArray(instanceClass.getInterfaces());

		Class superClass = instanceClass.getSuperclass();

		return OpenIterator.merge(OpenIterator.single(superClass), interfaces);
	}

	synchronized protected Map methodMap()
	{
		if (methods == null)
		{
			methods = new HashMap();
			Method[] mm = instanceClass.getMethods();
			for (int i = 0; i < mm.length; i++)
			{
				if (isPublic(mm[i].getDeclaringClass()))
				{
					JavaOpenMethod om = new JavaOpenMethod(mm[i]);
					methods.put(new MethodKey(om), om);
				}
			}

			Constructor[] cc = instanceClass.getConstructors();
			for (int i = 0; i < cc.length; i++)
			{
				if (isPublic(cc[i].getDeclaringClass()))
				{
					IOpenMethod om = new JavaOpenConstructor(cc[i]);
					//					Log.debug("Adding method " + mm[i].getName() + " code = " + new MethodKey(om).hashCode());
					methods.put(new MethodKey(om), om);
				}
			}

		}
		return methods;
	}

	boolean isPublic(Class declaringClass)
	{
		return Modifier.isPublic(declaringClass.getModifiers());
	}

	
	static class JavaClassClassField implements IOpenField
	{
	    Class<?> instanceClass;
	    
	    public JavaClassClassField(Class<?> instanceClass)
	    {
		this.instanceClass = instanceClass;
	    }

	    public Object get(Object target, IRuntimeEnv env)
	    {
		return instanceClass;
	    }

	    public boolean isConst()
	    {
		return true;
	    }

	    public boolean isReadable()
	    {
		return true;
	    }

	    public boolean isWritable()
	    {
		return false;
	    }

	    public void set(Object target, Object value, IRuntimeEnv env)
	    {
		throw new UnsupportedOperationException();
	    }

	    public IOpenClass getDeclaringClass()
	    {
		return null;
	    }

	    public IMemberMetaInfo getInfo()
	    {
		return null;
	    }

	    public IOpenClass getType()
	    {
		return JavaOpenClass.CLASS;
	    }

	    public boolean isStatic()
	    {
		return true;
	    }

	    public String getDisplayName(int mode)
	    {
		return "class";
	    }

	    public String getName()
	    {
		return "class";
	    }
	    
	}
	
	synchronized protected Map fieldMap()
	{
		if (fields == null)
		{
			fields = new HashMap();
			Field[] ff = instanceClass.getFields();

			for (int i = 0; i < ff.length; i++)
			{
				if (isPublic(ff[i].getDeclaringClass()))
					fields.put(ff[i].getName(), new JavaOpenField(ff[i]));
			}
			if (instanceClass.isArray())
				fields.put("length", new JavaArrayLengthField());
			
			fields.put("class", new JavaClassClassField(instanceClass));

			BeanOpenField.collectFields(fields, instanceClass);
		}
		return fields;
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenClass#isAbstract()
	 */
	public boolean isAbstract()
	{
		return Modifier.isAbstract(instanceClass.getModifiers());
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenClass#instanceOf(java.lang.Object)
	 */
	public boolean isInstance(Object instance)
	{
		return instanceClass.isInstance(instance);
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenClass#isAssignableFrom(java.lang.Class)
	 */
	public boolean isAssignableFrom(Class c)
	{
		return instanceClass.isAssignableFrom(c);
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenClass#isAssignableFrom(org.openl.types.IOpenClass)
	 */
	public boolean isAssignableFrom(IOpenClass ioc)
	{
		return instanceClass.isAssignableFrom(ioc.getInstanceClass());
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenClass#instanceClass()
	 */
	public Class getInstanceClass()
	{
		return instanceClass;
	}

	public Object nullObject()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see org.openl.base.INamedThing#getName()
	 */
	public String getName()
	{
		return instanceClass.getName();
	}

	//////////////////////// helpers ////////////////////////////

	static class Class2JavaOpenClassCollector implements IConvertor
	{
		public Object convert(Object obj)
		{
			return getOpenClass((Class) obj);
		}
	}

	static public final IConvertor Class2JavaOpenClass =
		new Class2JavaOpenClassCollector();

	static public IOpenClass[] getOpenClasses(Class[] cc)
	{
		if (cc.length == 0)
			return IOpenClass.EMPTY;

		IOpenClass[] ary = new IOpenClass[cc.length];

		CollectionsUtil.collect(ary, cc, Class2JavaOpenClass);

		return ary;

	}

	static class JavaPrimitiveClass extends JavaOpenClass
	{
		Class wrapperClass;
		Object nullObject;

		/**
		* @param instanceClass
		* @param schema
		* @param factory
		*/
		public JavaPrimitiveClass(
			Class instanceClass,
			Class wrapperClass,
			Object nullObject)
		{
			super(instanceClass, null);
			this.wrapperClass = wrapperClass;
			this.nullObject = nullObject;
		}

		public Object nullObject()
		{
			return nullObject;
		}

		/**
		 *
		 */

		public Object newInstance(IRuntimeEnv env)
		{
			return nullObject;
		}

	}

	static Map javaClassCache = null;

	public static final JavaOpenClass INT =
		new JavaPrimitiveClass(int.class, Integer.class, new Integer(0)),
		LONG = new JavaPrimitiveClass(long.class, Long.class, new Long(0)),
		DOUBLE =
			new JavaPrimitiveClass(double.class, Double.class, new Double(0)),
		FLOAT = new JavaPrimitiveClass(float.class, Float.class, new Float(0)),
		SHORT =
			new JavaPrimitiveClass(
				short.class,
				Short.class,
				new Short((short) 0)),
		CHAR =
			new JavaPrimitiveClass(
				char.class,
				Character.class,
				new Character('\0')),
		BYTE =
			new JavaPrimitiveClass(byte.class, Byte.class, new Byte((byte) 0)),
		BOOLEAN =
			new JavaPrimitiveClass(boolean.class, Boolean.class, Boolean.FALSE),
		VOID = new JavaPrimitiveClass(void.class, Void.class, null),
		STRING = new JavaOpenClass(String.class, null),
	   OBJECT = new JavaOpenClass(Object.class, null),
	   CLASS = new JavaOpenClass(Class.class, null);

	static synchronized Map getJavaClassCache()
	{
		if (javaClassCache == null)
		{
			javaClassCache = new HashMap();
			javaClassCache.put(int.class, INT);
			javaClassCache.put(long.class, LONG);
			javaClassCache.put(double.class, DOUBLE);
			javaClassCache.put(float.class, FLOAT);
			javaClassCache.put(short.class, SHORT);
			javaClassCache.put(char.class, CHAR);
			javaClassCache.put(byte.class, BYTE);
			javaClassCache.put(boolean.class, BOOLEAN);
			javaClassCache.put(void.class, VOID);
			javaClassCache.put(String.class, STRING);
			javaClassCache.put(Object.class, OBJECT);
			javaClassCache.put(Class.class, CLASS);
		}
		return javaClassCache;

	}

	public static synchronized void resetAllClassloaders(HashMap oldLoaders)
	{
		for (Iterator iter = oldLoaders.values().iterator(); iter.hasNext();)
		{
			ClassLoader cl = (ClassLoader) iter.next();
			resetClassloader(cl);
		}
	}
	

	public static synchronized void resetClassloader(ClassLoader cl)
	{
		Vector toRemove = new Vector();
		for (Iterator iter = getJavaClassCache().keySet().iterator(); iter.hasNext();)
		{
			Class c = (Class) iter.next();
			if (c.getClassLoader() == cl)
			  toRemove.add(c);
			  
		}
		
		for (Iterator iter = toRemove.iterator(); iter.hasNext();)
		{
			Class c = (Class)iter.next();
			javaClassCache.remove(c);
			
			//System.out.println("Removing " + printClass(c));
		}

	}



	public static synchronized void printCache()
	{
		int  i = 0;
		for (Iterator iter = getJavaClassCache().keySet().iterator(); iter.hasNext();)
		{
			Class element = (Class) iter.next();
			System.out.println("" + (i++) + ":\t"+  printClass(element));
			
		}
	}
	
	static String printClass(Class c)
	{
		if (c.isArray())
		  return "[]" + printClass(c.getComponentType());
		
		return c.getName();  
	}


	/**
	 * @param obj object or null, does not work on primitives
	 * @return JavaOpenClass for objects and NullOpenClass for null
	 */

	//	static public IOpenClass getOpenClass(Object obj)
	//	{
	//		if (obj == null)
	//			return NullOpenClass.the;
	//		return getOpenClass((Class)obj);  
	//	}	
	//

	static public synchronized JavaOpenClass getOpenClass(Class c)
	{
		JavaOpenClass res = (JavaOpenClass) getJavaClassCache().get(c);
		if (res == null)
		{
			res = new JavaOpenClass(c, null);
			getJavaClassCache().put(c, res);
		}

		return res;
	}

	public boolean equals(Object obj)
	{
		if (!(obj instanceof JavaOpenClass))
			return false;

		return instanceClass == ((JavaOpenClass) obj).instanceClass;
	}

	public int hashCode()
	{
		return instanceClass.hashCode();
	}

	/* (non-Javadoc)
	 * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String, org.openl.types.IOpenClass[])
	 */
	public IOpenMethod getMatchingMethod(String name, IOpenClass[] params)
		throws AmbiguousMethodException
	{
		return this.getMethod(name, params);
	}

	/* (non-Javadoc)
	 * @see org.openl.binding.IVarFactory#getVar(java.lang.String)
	 */
	public IOpenField getVar(String name)
	{
		return this.getField(name);
	}


	public static ArrayIndex makeArrayIndex(IOpenClass arrayType)
	{
		return new ArrayIndex(
			getOpenClass(arrayType.getInstanceClass().getComponentType()));
	}


	public static Class makeArrayClass(Class c)
	{
		return Array.newInstance(c, 0).getClass();
	}




	static class JavaArrayLengthField extends ArrayLengthOpenField
	{
		public int getLength(Object target)
		{
			return Array.getLength(target);
		}

	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenClass#getAggregateInfo()
	 */
	public IAggregateInfo getAggregateInfo()
	{
		return JavaArrayAggregateInfo.ARRAY_AGGREGATE ;
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenClass#newInstance()
	 */
	public Object newInstance(IRuntimeEnv env)
	{

		try
		{
			return instanceClass.newInstance();
		}
		catch (Exception e)
		{
			throw RuntimeExceptionWrapper.wrap(e);
		}
	}

	public String getDisplayName(int mode)
	{
		
		String name = getName();
		switch(mode)
		{
		case INamedThing.SHORT:
		case INamedThing.REGULAR:
			default:
			return StringTool.lastToken(name, ".");
		case INamedThing.LONG:
			return name;
		}
	}

	

}
