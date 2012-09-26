package org.openl.util.ce.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.util.TopoSort;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.IInvokableActivity;
import org.openl.util.ce.IScheduledActivity;
import org.openl.util.ce.IScheduler;
import org.openl.util.ce.conf.IServiceMTConfiguration;

public class Scheduler implements IScheduler {

	IServiceMTConfiguration config;

	public Scheduler(IServiceMTConfiguration config, long singleCellLength) {
		this.config = config;
		this.parLevel = config.getParallelLevel();
		this.minSeqLength = Math.max(
				(int) (config.getMinSequenceLengthNs() / singleCellLength), 1);
	}

	int parLevel = 4;
	int minSeqLength = 5;

	@Override
	public IScheduledActivity[] prepare(IActivity[] activities) {

		Map<IActivity, ScheduledActivity> all = prepareActivities(activities);

		List<ScheduledActivity> orderedActivities = orderActivities(all);

		markCriticalTime(orderedActivities, all);

		IScheduledActivity[] ary = orderedActivities
				.toArray(new IScheduledActivity[orderedActivities.size()]);
		Arrays.sort(ary, IScheduledActivity.comparator);

		markIds(ary);

		IScheduledActivity[] seqAry = makeSequences(ary);

		markIds(seqAry);

		return seqAry;
	}

	private IScheduledActivity[] makeSequences(IScheduledActivity[] ary) {

		boolean isInvokable = ary.length == 0
				|| ary[0].activity() instanceof IInvokableActivity;

		if (ary.length <= minSeqLength * 2) {
			return new IScheduledActivity[] { new SequenceActivity(ary,
					isInvokable) };
		}

		int nParBuckets = Math.min(ary.length / minSeqLength, parLevel);

		List<IScheduledActivity>[] buckets = new List[nParBuckets];
		List<IScheduledActivity> extraBucket = new ArrayList<IScheduledActivity>();
		

		for (int i = 0; i < buckets.length; i++) {
			buckets[i] = new ArrayList<IScheduledActivity>();
		}

		for (int i = 0; i < ary.length; i++) {
			if (ary[i].getPrecedentSize() == 0)
				selectMinSizeBucket(buckets).add(ary[i]);
			else if (ary[i].getPrecedentSize() == 1)
				selectBucketWithPrecedentOf(ary[i], buckets, extraBucket).add(ary[i]);
			else selectBucketWIthAllPrecedentsOf(ary[i], buckets, extraBucket).add(ary[i]);

		}

		List<IScheduledActivity> list = new ArrayList<IScheduledActivity>();
		SequenceActivity extraAct = extraBucket.size() > 0 ? 
				new SequenceActivity(extraBucket.toArray(new IScheduledActivity[0]), isInvokable) : null;
		for (int i = 0; i < buckets.length; i++) {
			if (buckets[i].size() == 0) continue;
			IScheduledActivity newAct = new SequenceActivity(buckets[i].toArray(new IScheduledActivity[0]), isInvokable);
			
			if (extraAct != null)
			{	
				newAct.getDependents().add(extraAct);
				extraAct.setPrecedentSize(extraAct.getPrecedentSize() + 1);
			}
			
			list.add(newAct);
			
		}
		
		if (extraAct != null)
			list.add(extraAct);
		
		return list.toArray(new IScheduledActivity[0]);
	}

	private List<IScheduledActivity> selectBucketWIthAllPrecedentsOf(
			IScheduledActivity iScheduledActivity,
			List<IScheduledActivity>[] buckets,
			List<IScheduledActivity> extraBucket) {

		for (int i = 0; i < buckets.length; i++) {
			int sumPrecedents = 0;
			for (IScheduledActivity act : buckets[i]) {
				if (act.getDependents().contains(iScheduledActivity))
					sumPrecedents++;
			}
			if (sumPrecedents == iScheduledActivity.getPrecedentSize())
				return buckets[i];
		}

		return extraBucket;
		
	}

	private List<IScheduledActivity> selectBucketWithPrecedentOf(
			IScheduledActivity iScheduledActivity,
			List<IScheduledActivity>[] buckets, List<IScheduledActivity> extraBucket) {
		for (int i = 0; i < buckets.length; i++) {
			for (IScheduledActivity act : buckets[i]) {
				if (act.getDependents().contains(iScheduledActivity))
					return buckets[i];
			}
		}
		return extraBucket;
	}

	private List<IScheduledActivity> selectMinSizeBucket(
			List<IScheduledActivity>[] buckets) {
		int min = 0;
		for (int i = 1; i < buckets.length; i++) {
			if (buckets[i].size() < buckets[min].size()) {
				min = i;
			}
		}

		return buckets[min];

	}

	protected static void markIds(IScheduledActivity[] orderedActivities) {
		int i = 0;
		for (IScheduledActivity scheduledActivity : orderedActivities) {
			scheduledActivity.setId(i++);
		}
	}

	private void markCriticalTime(List<ScheduledActivity> orderedActivities,
			Map<IActivity, ScheduledActivity> all) {
		int size = orderedActivities.size();

		for (int i = 0; i < size; i++) {
			ScheduledActivity act = orderedActivities.get(size - 1 - i);

			boolean changed = act.increaseCriticalDistanceFromDependent(0);

			if (!changed)
				continue;

			updateActivityPredecessors(act, all);

		}
	}

	private void updateActivityPredecessors(ScheduledActivity act,
			Map<IActivity, ScheduledActivity> all) {

		List<IActivity> dependsOn = act.activity().dependsOn();
		if (dependsOn == null)
			return;

		for (IActivity iActivity : dependsOn) {
			ScheduledActivity iact = all.get(iActivity);

			boolean changed = iact.increaseCriticalDistanceFromDependent(act
					.getCriticalDistance());

			if (!changed)
				continue;

			updateActivityPredecessors(iact, all);
		}
	}

	/**
	 * Orders activities in topological order and sets dependents
	 * 
	 * @param all
	 * @return
	 */

	private List<ScheduledActivity> orderActivities(
			Map<IActivity, ScheduledActivity> all) {

		TopoSort<ScheduledActivity> ts = new TopoSort<ScheduledActivity>();

		for (ScheduledActivity root : all.values()) {
			ts.addOrderedPair(root, null);
		}

		for (ScheduledActivity leaf : all.values()) {

			List<IActivity> dependsOn = leaf.activity().dependsOn();

			if (dependsOn != null)
				for (IActivity ia : dependsOn) {
					ScheduledActivity root = all.get(ia);
					root.addDependent(leaf);
					ts.addOrderedPair(root, leaf);
				}
		}

		return ts.sort();
	}

	private Map<IActivity, ScheduledActivity> prepareActivities(
			IActivity[] activities) {

		Map<IActivity, ScheduledActivity> map = new HashMap<IActivity, ScheduledActivity>(
				activities.length);

		for (IActivity activity : activities) {
			map.put(activity, new ScheduledActivity(activity));
		}

		return map;
	}

}
