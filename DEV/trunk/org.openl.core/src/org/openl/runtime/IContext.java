package org.openl.runtime;

import org.openl.meta.VariableDefinition;

/**
 * This interface represent the runtime context abstraction what can be accessed
 * and modified by user for user-defined variables or values.
 * 
 * Runtime context used by OpenL tablets engine for rules overload support.
 * 
 * 
 * @author Alexey Gamanovich
 */
public interface IContext {
	
	/**
	 * Adds new value to context.
	 * 
	 * @param name
	 *            value name
	 * @param clazz
	 *            value type
	 * @param value
	 *            variable value
	 */
	void addValue(String name, Class clazz, Object value);
	
	/**
	 * Removes value from context.
	 * 
	 * @param name
	 *            value name
	 * @param clazz
	 *            value type
	 */
	void removeValue(String name, Class clazz);
	
	/**
	 * Gets the value from context.
	 * 
	 * @param name
	 *            value name
	 * @param clazz
	 *            value type
	 * @return value
	 */
	Object getValue(String name, Class clazz);
	
	/**
	 * Sets the new value to context.
	 * 
	 * @param name
	 *            value name
	 * @param clazz
	 *            value type
	 * @param value
	 *            new value
	 */
	void setValue(String name, Class clazz, Object value);
}
