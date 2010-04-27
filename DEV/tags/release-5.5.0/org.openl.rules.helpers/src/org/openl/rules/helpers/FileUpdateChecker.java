package org.openl.rules.helpers;

import java.io.File;

public class FileUpdateChecker 
{

	String[] names;
	long[] timestamps;
	
	public FileUpdateChecker(String[] fnames)
	{
		names = fnames;
		timestamps = new long[names.length];
	}
	
	public boolean isUpdated()
	{
		boolean updated = false;
		
		for (int i = 0; i < names.length; i++) 
		{
			File f = new File(names[i]);
			
			long modified = f.lastModified();
			if (modified != timestamps[i])
			{
				timestamps[i] = modified;
				updated = true;
			}	
			
		}
		
		return updated;
	}
	
}
