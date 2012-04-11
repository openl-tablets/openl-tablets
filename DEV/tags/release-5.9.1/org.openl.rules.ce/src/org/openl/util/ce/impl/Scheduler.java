package org.openl.util.ce.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.util.TopoSort;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.IScheduledActivity;
import org.openl.util.ce.IScheduler;

@SuppressWarnings("rawtypes")
public class Scheduler implements IScheduler{

	@Override
	public List<IScheduledActivity<?>> prepare(List<IActivity<?>> activities) {
		
		Map<IActivity, ScheduledActivity> all = prepareActivities(activities);
		
		
		List<ScheduledActivity> orderedActivities = orderActivities(all);

		markCriticalTime(orderedActivities, all);
		
		
		// TODO Auto-generated method stub
		return null;
	}

	private void markCriticalTime(List<ScheduledActivity> orderedActivities, Map<IActivity, ScheduledActivity> all) {
		int size = orderedActivities.size();
		
		for (int i = 0; i < size; i++) {
			ScheduledActivity<?> act = orderedActivities.get(size-1-i);

			boolean changed  = act.increaseCriticalDistanceFromDependent(0);
			
			if (!changed) continue;
			
			updateActivityPredecessors(act, all);
			
		}
	}
	
	
	private void updateActivityPredecessors(ScheduledActivity act, Map<IActivity, ScheduledActivity> all)
	{
		
		List<IActivity> dependsOn  = act.activity().dependsOn();
		if (dependsOn == null) return;
			
		
		for (IActivity<?> iActivity : dependsOn) {
			ScheduledActivity<?> iact = all.get(iActivity);
			
			boolean changed = iact.increaseCriticalDistanceFromDependent(act.getCriticalDistance());
			
			if (!changed) continue;
			
			updateActivityPredecessors(iact, all);
		} 
	}
	
	

	private List<ScheduledActivity> orderActivities(
			Map<IActivity, ScheduledActivity> all) {
		
		TopoSort<ScheduledActivity> ts = new TopoSort<ScheduledActivity>();
		
		for (ScheduledActivity root : all.values()) {
			
			@SuppressWarnings("unchecked")
			List<IActivity> dependsOn = root.activity().dependsOn();
			
			if (dependsOn != null)
			for (IActivity<?> ia : dependsOn) {
				ScheduledActivity<?> leaf = all.get(ia);
				ts.addOrderedPair(root, leaf);
			}
		}
		
		
		return ts.sort();
	}

	private Set<IScheduledActivity<?>> findRoots(
			Map<IActivity<?>, ScheduledActivity<?>> all) {
		
		Set<IScheduledActivity<?>> set = new HashSet<IScheduledActivity<?>>(all.size());
		
		for (Map.Entry<IActivity<?>,ScheduledActivity<?>> e : all.entrySet()) {
			
		}
		
		return set;
	}

	private Map<IActivity, ScheduledActivity> prepareActivities(
			List<IActivity<?>> activities) {
		
		Map<IActivity, ScheduledActivity> map = new HashMap<IActivity, ScheduledActivity>(activities.size());
		
		
		for (IActivity<?> activity : activities) {
			map.put(activity, new ScheduledActivity(activity));
		}
		
		
		return map;
	}
	
	
	

}
