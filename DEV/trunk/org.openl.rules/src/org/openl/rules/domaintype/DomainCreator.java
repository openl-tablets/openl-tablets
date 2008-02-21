package org.openl.rules.domaintype;

import org.openl.domain.EnumDomain;
import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;

public class DomainCreator
{
	String name;

	IOpenClass type;

	IntRange range;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public IOpenClass getType()
	{
		return type;
	}

	public void setType(IOpenClass type)
	{
		this.type = type;
	}

	public IntRange getRange()
	{
		return range;
	}

	public void setRange(IntRange range)
	{
		this.range = range;
	}

	public IOpenClass makeDomain()
	{
		return new DomainOpenClass(name, type, range, null);
	}
	
	static public EnumDomain makeEnum(Object data)
	{
		
		Class<?> clazz = data.getClass();
		
		if (!clazz.isArray())
		{
			throw new RuntimeException("Domain class can only be created from array");
		}	

		if (clazz.getComponentType().isPrimitive())
		{
			throw new RuntimeException("Enum can not be created from primitives (temporary limitation)");
		}	
		
		EnumDomain ed = new EnumDomain((Object[]) data);
		
		return ed;
		
	}

}
