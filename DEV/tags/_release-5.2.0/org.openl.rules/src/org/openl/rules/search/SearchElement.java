/**
 * Created May 4, 2007
 */
package org.openl.rules.search;



/**
 * @author snshor
 *
 */
public class SearchElement implements ISearchConstants
{

	GroupOperator operator = new GroupOperator.AND();
	boolean notFlag = false;
	protected String type;
	String opType1;
	String value1 = ANY;
	String opType2;
	String value2 = ANY;
	
	
	public boolean isAny(String value)
	{
		return value == null || value.trim().length() == 0 || ANY.equals(value);
	}
	
	public SearchElement(String type)
	{
		this.type = type;
	}
	
	public boolean isNotFlag()
	{
		return this.notFlag;
	}
	public void setNotFlag(boolean notFlag)
	{
		this.notFlag = notFlag;
	}
	public GroupOperator getOperator()
	{
		return this.operator;
	}
	public void setOperator(GroupOperator operator)
	{
		this.operator = operator;
	}
	public String getOpType2()
	{
		return this.opType2;
	}
	public void setOpType2(String opType)
	{
		this.opType2 = opType;
	}
	public String getType()
	{
		return this.type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public String getValue1()
	{
		return this.value1;
	}
	public void setValue1(String value1)
	{
		this.value1 = value1;
	}
	
	public String getValue2()
	{
		return this.value2;
	}
	public void setValue2(String value2)
	{
		this.value2 = value2;
	}
	
	public SearchElement copy()
	{
		SearchElement cpy = new SearchElement(type);
		
		cpy.notFlag = notFlag;
		cpy.operator = operator;
		cpy.opType1 = opType1;
		cpy.value1 = value1;
		cpy.opType2 = opType2;
		cpy.value2 = value2;
		
		return cpy;
	}

	public String getOpType1()
	{
		return this.opType1;
	}

	public void setOpType1(String opType1)
	{
		this.opType1 = opType1;
	}
}
