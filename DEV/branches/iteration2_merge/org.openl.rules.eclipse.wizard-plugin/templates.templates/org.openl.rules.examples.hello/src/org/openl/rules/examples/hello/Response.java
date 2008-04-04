/*
 * Created on Oct 24, 2003
 *
 * Developed by OpenRules Inc. 2003
 */
package org.openl.rules.examples.hello;

import java.util.HashMap;

/**
 * @author Jacob
 *
 */
public class Response
{
	public Response()
	{
	}

	protected String result;
	public String getResult()
	{
		return result;
	}
	public void setResult(String s)
	{
		result = s;
	}
	
	HashMap map = new HashMap();

	public HashMap getMap()
	{
		return map;
	}

}
