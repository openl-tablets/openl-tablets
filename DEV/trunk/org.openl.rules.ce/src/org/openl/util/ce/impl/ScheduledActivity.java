package org.openl.util.ce.impl;

import java.util.ArrayList;
import java.util.List;

import org.openl.util.IdObject;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.IScheduledActivity;

public class ScheduledActivity extends IdObject implements IScheduledActivity {

	@Override
	public String toString() {
		
		
		return super.toString() + "/" + criticalDistance + "/" + activity + "/" 
//		+ isOnCriticalPath 
//		+ dependents;
				+ printDepIds(dependents);
	}


	private String printDepIds(List<IScheduledActivity> deps) {
		StringBuilder buf = new StringBuilder();
		boolean first = true;
		
		buf.append('{');
		for (IScheduledActivity act : deps) {
			if (!first) buf.append(",");
			buf.append(act.getId());
			first = false;
		}
		buf.append('}');
		
		return buf.toString();
	}


	private IActivity activity;
	private long criticalDistance;
	private boolean isOnCriticalPath;

	public ScheduledActivity(IActivity activity) {
		this.activity = activity;
	}

	@Override
	public IActivity activity() {
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


	List<IScheduledActivity> dependents = new ArrayList<IScheduledActivity>();
	
	public List<IScheduledActivity> getDependents() {
		return dependents;
	}

	public void addDependent(ScheduledActivity dep) {
		dependents.add(dep);
	}


	@Override
	public int getPrecedentSize() {
		return activity.dependsOn().size();
	}
	
	
	

}
