/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types;

import java.util.Iterator;

import org.openl.binding.IOpenLibrary;
import org.openl.domain.IType;
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

public interface IOpenClass extends IType, IOpenLibrary, IOpenClassHolder, IMetaHolder
{
	
	/**
	 * @return the schema it has been created with
	 */
	public IOpenSchema getSchema();
			
	
	
	/**
	 * We do not have a limitation on number of superclasses.
	 * This feature is not fully supported yet
	 */
	public Iterator<IOpenClass> superClasses();
	
	/**
	 * @return
	 */
	public Iterator<IOpenMethod> methods();

	/**
	 * @return an iterator of all the fieldValues
	 */
	public Iterator<IOpenField> fields(); 

	
	/**
	 * This method returns a class field by it's name; in case of strictMatch
	 * the name search will be case-sensitive. In case of strictMatch == false,
	 * the search will be case-insensitive and may produce AmbiguousVarException.
	 * In the future we might implement more sophisticated matching techniques to help
	 * users to deal with typos in their code   
	 * 
	 * @param name
	 * @param strictMatch
	 * @return
	 * @since 5.0 
	 */
	public IOpenField getField(String name, boolean strictMatch);
	
	
	public IOpenField getField(String name);
	
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
	public boolean isAssignableFrom(Class<?> c);

	/**
	 * @return the actual Java implementation of the instance, should return primitive classes in case of int, char etc. 
	 */
	
	public Class<?> getInstanceClass();
	
	
	public IOpenMethod getMethod(String name, IOpenClass[] classes);
	
	
	
	

	public static final IOpenClass[] EMPTY = {};
	
	IOpenField getIndexField();

	/**
	 * This is analog of Java Class.isPrimitive(). We have a little bit different perspective on the things,
	 * and consider classes as java.util.Date, java.lang.String and java.lang.Class also primitive. In general,
	 * a class is considered primitive if  
	 * a) it can be easily serialized/deserialized to string 
	 * b) does not have read/write fieldValues in java.beans sense
	 * 
	 * One of the use of the simple classes is that they provide "terminal" points
	 * in object graph discovery. 
	 * 
	 * 
	 *  We do not provide (yet) an automatic discovery mechanism for "simple" classes, so it is developer's
	 *  responsibility to extends OpenL set of simple classes with his own
	 * @return
	 */

	public boolean isSimple();
	
}
