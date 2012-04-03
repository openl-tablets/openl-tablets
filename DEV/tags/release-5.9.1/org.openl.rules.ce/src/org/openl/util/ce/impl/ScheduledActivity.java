package org.openl.util.ce.impl;

import org.openl.util.ce.IActivity;
import org.openl.util.ce.IScheduledActivity;

public class ScheduledActivity<T> implements IScheduledActivity<T> {

	private IActivity<T> activity;
	private long criticalDistance;
	private boolean isOnCriticalPath;

	public ScheduledActivity(IActivity<T> activity) {
		this.activity = activity;
	}

	@Override
	public IActivity<T> activity() {
		return activity;
	}

	public long getCriticalDistance() {
		return criticalDistance;
	}

	public void setCriticalDistance(long criticalDistance) {
		this.criticalDistance = criticalDistance;
	}

	public boolean isOnCriticalPath() {
		return isOnCriticalPath;
	}

	public void setOnCriticalPath(boolean isOnCriticalPath) {
		this.isOnCriticalPath = isOnCriticalPath;
	}
	
	
	/**
	 * The critical distance should be max(cd(dependents)) + duration 
	 * 
	 * @param depententDistance
	 * @return true if critical distance has changed
	 */
	
	public boolean increaseCriticalDistanceFromDependent(long dependentDistance)
	{
		long candidate = dependentDistance + activity.duration();
		
		if (candidate > criticalDistance)
		{
			setCriticalDistance(candidate);
			return true;
		}
		
		return false;
	}
	
	
	

}
