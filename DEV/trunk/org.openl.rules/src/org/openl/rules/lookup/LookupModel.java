package org.openl.rules.lookup;

public class LookupModel
{
	
	ISingleLookupModel lookupModel;
	
	public Object lookup(Object[] lookups)
	{
		ISingleLookupModel lookup = lookupModel;
		
		int size = lookups.length;
		
		for (int i = 0; i < size-1; i++)
		{
			lookup = (ISingleLookupModel)lookup.find(lookups[i]);
		}
		
		return lookup.find(lookups[size-1]);
	}
}
