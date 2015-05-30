package org.openl.util.trie;

public interface ISequentialKey {
	
	int length();
	int keyAt(int position);
	
	
	/**
	 * 
	 * @param position
	 * @return index information
	 */
	
	KeyRange keyRange(int position);

	public interface KeyRange
	{
		
		int initialMin();
		int initialMax();
		
		int absMin();
		int absMax();
	}
	

}
