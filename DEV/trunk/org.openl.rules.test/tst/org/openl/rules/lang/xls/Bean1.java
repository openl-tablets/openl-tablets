/*
 * Created on Oct 24, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.lang.xls;

/**
 * @author snshor
 *
 */
public class Bean1
{

	public Bean1(String s)
	{
		throw new RuntimeException("zopa");
	}
	
	public Bean1(){}

	String key, name;
	
	
	int intvalue;
	Bean1 bref;		

	/**
	 * @return
	 */
	public String getKey()
	{
		return key;
	}


	/**
	 * @param string
	 */
	public void setKey(String string)
	{
		key = string;
	}


	/**
	 * @return
	 */
	public int getIntvalue()
	{
		return intvalue;
	}

	/**
	 * @return
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param i
	 */
	public void setIntvalue(int i)
	{
		intvalue = i;
	}

	/**
	 * @param string
	 */
	public void setName(String string)
	{
		name = string;
	}

	/**
	 * @return
	 */
	public Bean1 getBref()
	{
		return bref;
	}

	/**
	 * @param bean1
	 */
	public void setBref(Bean1 bean1)
	{
		bref = bean1;
	}
	
	public String toString()
	{
		return key + ":" + name + ":" + intvalue + ":" + (bref == null ? "n/a" : bref.key);
	}

}
