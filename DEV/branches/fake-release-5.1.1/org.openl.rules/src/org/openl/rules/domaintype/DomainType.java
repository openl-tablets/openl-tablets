package org.openl.rules.domaintype;

import org.openl.meta.StringValue;

/**
 * 
 * @author snshor
 *
 */

public class DomainType
{
	StringValue domainName;
	StringValue baseType;
	StringValue varName;
	StringValue useVarPattern;
	StringValue description;
	StringValue displayName;
	StringValue domainExpression;
	
	public StringValue getDomainName()
	{
		return domainName;
	}
	public void setDomainName(StringValue domainName)
	{
		this.domainName = domainName;
	}
	public StringValue getBaseType()
	{
		return baseType;
	}
	public void setBaseType(StringValue baseType)
	{
		this.baseType = baseType;
	}
	public StringValue getVarName()
	{
		return varName;
	}
	public void setVarName(StringValue varName)
	{
		this.varName = varName;
	}
	public StringValue getUseVarPattern()
	{
		return useVarPattern;
	}
	public void setUseVarPattern(StringValue useVarPattern)
	{
		this.useVarPattern = useVarPattern;
	}
	public StringValue getDescription()
	{
		return description;
	}
	public void setDescription(StringValue description)
	{
		this.description = description;
	}
	public StringValue getDisplayName()
	{
		return displayName;
	}
	public void setDisplayName(StringValue displayName)
	{
		this.displayName = displayName;
	}
	public StringValue getDomainExpression()
	{
		return domainExpression;
	}
	public void setDomainExpression(StringValue domainExpression)
	{
		this.domainExpression = domainExpression;
	}
}
