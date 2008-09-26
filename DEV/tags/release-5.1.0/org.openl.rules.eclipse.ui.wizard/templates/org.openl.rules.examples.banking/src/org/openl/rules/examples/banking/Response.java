/*
 * Created on Oct 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
package org.openl.rules.examples.banking;

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

	protected String comment;
	public String getComment()
	{
		return comment;
	}
	
	protected String[] products;
	
	public void setComment(String s)
	{
		comment = s;
	}
	
	HashMap map = new HashMap();

	public HashMap getMap()
	{
		return map;
	}
	/**
	 * @return
	 */
	public String[] getProducts()
	{
		if (products == null)
			products = new String[0];
		return products;
	}

	/**
	 * @param strings
	 */
	public void setProducts(String[] strings)
	{
		products = strings;
	}
	
	public String toString()
	{
		StringBuffer buf = new StringBuffer(2500);

		buf.append("Offered Products:").append("\n");
		for(int i=0; i<getProducts().length; ++i)
		{
			buf.append("\t").append(getProducts()[i]).append("\n");
		}
		buf.append("Comment: ").append(comment).append("\n");

		return buf.toString();
	}

}
