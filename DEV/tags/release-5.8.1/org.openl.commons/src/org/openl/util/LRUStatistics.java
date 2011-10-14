package org.openl.util;

public class LRUStatistics {
	
	public int hit;
	public int miss;
	public int removed;
	
	
	@Override
	public String toString() {
		return "LRUStatistics [hit=" + hit + ", miss="
				+ miss + ", removed=" + removed + "]";
	}
	
	

}
