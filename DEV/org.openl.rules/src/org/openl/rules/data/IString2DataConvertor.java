/*
 * Created on Nov 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.data;

/**
 * @author snshor
 *
 */
public interface IString2DataConvertor
{
	public Object parse(String data, String format);

//	public Object convertArray(String[] data);

	public String format(Object data, String format);

//	public Object makeArray(int size);

}
