package org.openl.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openl.meta.VariableDefinition;

/**
 * <code>IContext</code> implementation based on <code>HashMap</code>.
 * 
 * @author Alexey Gamanovich
 */
public class HashMapContext implements IContext {
	
	private Map<VariableDefinition, Object> vars = new HashMap<VariableDefinition, Object>();
	
	/**
	 * Add new value to context. If the variable with the same name and class
	 * already exists it will be overridden.
	 * 
	 * @param name
	 *            variable name
	 * @param clazz
	 *            variable type
	 * @param value
	 *            variable value
	 */
	public void addValue(String name, Class clazz, Object value) {
		
		// Checks that type of variable and its value are same.
		//
		if (!clazz.isInstance(value)) {
			throw new RuntimeException("The type " + value.getClass().getName() + " cannot be casted to "
			        + clazz.getName());
		}
		
		// Creates new definition.
		//
		VariableDefinition definition = new VariableDefinition(name, clazz);
		
		// Stores new variable and its value in internal map.
		//
		vars.put(definition, value);
	}
	
	/**
	 * Removes the variable from context.
	 * 
	 * @param name
	 *            variable name
	 * @param clazz
	 *            variable type
	 */
	public void removeValue(String name, Class clazz) {
		
		VariableDefinition definition = findKey(name, clazz);
		
		// Checks that internal map has the variable. If it's true then remove
		// variable from map; otherwise - do nothing.
		//
		if (definition != null) {
			vars.remove(definition);
		}
	}
	
	/**
	 * Sets the variable value. If variable does not exist
	 * <code>RuntimeException</code> will be thrown.
	 * 
	 * @see #addValue(VariableDefinition, Object)
	 * 
	 * @param definition
	 *            variable definition
	 * @param value
	 *            variable value
	 */
	public void setValue(String name, Class clazz, Object value) {
		
		if (!containsKey(name, clazz)) {
			throw new RuntimeException("The specified varible does not exist");
		}
		
		addValue(name, clazz, value);
	}
	
	/**
	 * Gets the value of variable. If the given variable does not exist
	 * <code>null</code> will be returned.
	 * 
	 * @param name
	 *            variable name
	 * @param clazz
	 *            variable type
	 * @return variable value
	 */
	public Object getValue(String name, Class clazz) {
		
		VariableDefinition definition = findKey(name, clazz);
		
		if (definition != null) {
			return vars.get(definition);
		}
		
		return null;
	}
	
	/**
	 * Finds key in internal map of variables using name and type of variable.
	 * 
	 * @param name
	 *            variable name
	 * @param clazz
	 *            variable type
	 * @return {@link VariableDefinition} instance if key founded; null -
	 *         otherwise
	 */
	private VariableDefinition findKey(String name, Class clazz) {
		
		Set<VariableDefinition> definitions = vars.keySet();
		
		for (VariableDefinition definition : definitions) {
			
			String varName = definition.getName();
			Class varClazz = definition.getClazz();
			
			// Checks the name and type of variable. If definition that has the
			// same name value and class value then return this.
			//
			if (varName.equals(name) && varClazz == clazz) {
				return definition;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks that entry with given name and type already contained.
	 * 
	 * @param name
	 *            variable name
	 * @param clazz
	 *            variable type
	 * @return <code>true</code> if entry already exists; <code>false</code> -
	 *         otherwise
	 */
	public boolean containsKey(String name, Class clazz) {
		return findKey(name, clazz) != null;
	}
}
