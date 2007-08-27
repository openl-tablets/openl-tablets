/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding;

import java.util.Iterator;
import java.util.Vector;

import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public class AmbiguousVarException extends RuntimeException
{


	/**
	 * 
	 */
	private static final long serialVersionUID = -8752617383143899614L;
	Vector matchingFields; 
	String varName; 
	
	public AmbiguousVarException( String varName, Vector matchingFields)
	{
		this.varName = varName;
		this.matchingFields = matchingFields; 
	}
	
	public String getMessage()
	{
		StringBuffer buf = new StringBuffer();
		
		buf.append("Variable ").append(varName);
		buf.append(" is ambigous:\n").
		append("Matching fields:\n");
		for (Iterator iter = matchingFields.iterator(); iter.hasNext();)
    {
			IOpenMethod method = (IOpenMethod)iter.next();
      MethodUtil.printMethod(method, buf).append('\n');
    }
		
		
		return buf.toString();
	}



}
