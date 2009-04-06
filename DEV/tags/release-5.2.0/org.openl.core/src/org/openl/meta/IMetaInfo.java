package org.openl.meta;

public interface IMetaInfo
{
	static public  final 
		int SHORT = 0, REGULAR = 1, LONG = 2;
	
	
	public String getDisplayName(int mode);
	
	public String getSourceUrl();
	
}
