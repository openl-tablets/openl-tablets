package org.openl.util.ce;

import java.util.List;

public interface IDependent<T> {
	
	/**
	 * 
	 * @return a list of activities that must precede this activity.  
	 */
	
	List<T> dependsOn();

}
