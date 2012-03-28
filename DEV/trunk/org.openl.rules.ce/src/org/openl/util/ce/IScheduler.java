package org.openl.util.ce;

import java.util.List;

public interface IScheduler {

	List<IScheduledActivity<?>>  prepare(List<IActivity<?>> activities); 
	
}
