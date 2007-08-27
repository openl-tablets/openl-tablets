/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types;

import java.util.Iterator;

import org.openl.base.INamedThing;
import org.openl.binding.IOpenLibrary;
import org.openl.meta.IMetaHolder;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 * OpenClass represents a generalized abstraction of a "class".
 * Because we want openL to be used in a many different incarnations we
 * have made a decision to keep an OpenClass as general as possible. It should be close in spirit
 * to the RDF:Class and OWL:Class.
 * @TODO put some href here
 *
 */

public interface IOpenClass extends INamedThing, IOpenLibrary, IOpenClassHolder, IMetaHolder
{
	
	/**
	 * @return the schema it has been created with
	 */
	public IOpenSchema getSchema();
			
	
	
	/**
	 * We do not have a limitation on number of superclasses
	 */
	public Iterator superClasses();
	
	/**
	 * @return
	 */
	public Iterator methods();

	/**
	 * @return
	 */
	public Iterator fields(); 
	
	public Object nullObject();



	/**
	 * @return true if the instance of the class can not be created
	 */
	public boolean isAbstract();


	public IAggregateInfo getAggregateInfo();
	

	
	//********* instance related methods ***********//
	
	
	public Object newInstance(IRuntimeEnv env);
	
	/**
	 * @return true if instance is one of the class or it's superclasses. 
	 * Warning: there may be a confusion in case of Java primitives. Java spec says that a corresponding
	 * method of java.lang.Class will return false in case of primitive types.
	 */
	public boolean isInstance(Object instance);	
	
	/**
	 * @param ioc  IOpenClass to check
	 * @return true if the instance of corresponding IOpenClass class belongs to the open class. 
	 */
	public boolean isAssignableFrom(IOpenClass ioc);
	
	/**
	 * @param c  Class to check
	 * @return true if the instance of corresponding Class class belongs to the open class. 
	 */
	public boolean isAssignableFrom(Class c);

	/**
	 * @return the actual Java implementation of the instance, should return primitive classes in case of int, char etc. 
	 */
	
	public Class getInstanceClass();
	
	
	public IOpenMethod getMethod(String name, IOpenClass[] classes);
	
	public IOpenField getField(String name);
	

	public static final IOpenClass[] EMPTY = {};
	
	IOpenField getIndexField();
	
}
