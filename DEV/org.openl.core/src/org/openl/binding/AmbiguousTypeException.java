/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding;

import java.util.Iterator;
import java.util.Vector;

import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class AmbiguousTypeException extends RuntimeException
{


	/**
	 * 
	 */
	private static final long serialVersionUID = 3432594431020887309L;
	Vector matchingTypes; 
	String typeName; 
	
	public AmbiguousTypeException( String typeName, Vector matchingFields)
	{
		this.typeName = typeName;
		this.matchingTypes = matchingFields; 
	}
	
	public String getMessage()
	{
		StringBuffer buf = new StringBuffer();
		
		buf.append("Type ").append(typeName);
		buf.append(" is ambigous:\n").
		append("Matching types:\n");
		for (Iterator iter = matchingTypes.iterator(); iter.hasNext();)
    {
			IOpenClass type = (IOpenClass)iter.next();
			buf.append(type.getName()).append('\n');
    }
		
		
		return buf.toString();
	}



}
