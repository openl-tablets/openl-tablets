package org.openl.meta;

/**
 * Class that describes the variable entity. Used as meta information of
 * variable and provides the following information: variable name, variable
 * type.
 * 
 * @author Alexey Gamanovich
 */
public class VariableDefinition {
	
	/**
	 * Variable name.
	 */
	private String name;
	
	/**
	 * Variable type.
	 */
	private Class<?> clazz;
	
	/**
	 * Creates new instance of class using the name, value and type of variable.
	 * 
	 * @param name
	 *            variable name
	 * @param value
	 *            variable value
	 * @param clazz
	 *            type of variable
	 */
	public VariableDefinition(String name, Class<?> clazz) {
		
		this.name = name;
		this.clazz = clazz;
	}
	
	/**
	 * Gets the variable name.
	 * 
	 * @return string that represents the variable name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the variable type.
	 * 
	 * @return <code>Class</code> instance that represents the variable type
	 */
	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * Gets hash code using name and clazz values.
	 */
	@Override
    public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
	    result = prime * result + ((name == null) ? 0 : name.hashCode());
	    return result;
    }

	@Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    VariableDefinition other = (VariableDefinition) obj;
	    if (clazz == null) {
		    if (other.clazz != null)
			    return false;
	    } else if (clazz != other.clazz)
		    return false;
	    if (name == null) {
		    if (other.name != null)
			    return false;
	    } else if (!name.equals(other.name))
		    return false;
	    return true;
    }
}
