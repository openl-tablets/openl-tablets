package org.openl.util.ce;

import java.util.List;

public interface IPredecessor<T> {

	List<T> dependents(); 
	
}
