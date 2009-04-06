package org.openl.rules.lookup;

import java.util.HashMap;

public class MapLookupFactory implements ISingleLookupFactory
{

	public ISingleLookupModel makeModel(Object[] lookups, Object[] values)
	{
		HashMap map = new HashMap();
		for (int i = 0; i < lookups.length; i++)
		{
			map.put(lookups[i], values[i]);
		}
		
		return new MapLookupModel(map);
	}

}
