/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding;


/**
 * @author snshor
 *
 */
public class DuplicatedVarException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2754037692502108330L;
	String msg; 
	String fieldName; 
	
	public DuplicatedVarException(String msg, String fieldName)
	{
		this.msg = msg;
		this.fieldName = fieldName;
	}
	
	public String getMessage()
	{
		StringBuffer buf = new StringBuffer();
		if (msg != null)
		  buf.append(msg);
		
		buf.append("Var ").append(fieldName);
    buf.append(" has already been defined");
    return buf.toString();
	}

}
