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
public class FieldNotFoundException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -782077307706500730L;
	String msg; 
	String fieldName; 
	
	public FieldNotFoundException(String msg, String fieldName)
	{
		this.msg = msg;
		this.fieldName = fieldName;
	}
	
	public String getMessage()
	{
		StringBuffer buf = new StringBuffer();
		if (msg != null)
		  buf.append(msg);
		
		buf.append("Field ").append(fieldName);
    buf.append(" is not found");
    return buf.toString();
	}

}
