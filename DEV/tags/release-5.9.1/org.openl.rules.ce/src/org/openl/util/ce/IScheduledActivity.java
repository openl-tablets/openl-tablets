package org.openl.util.ce;

import java.util.Comparator;

public interface IScheduledActivity<T> {

	
	IActivity<T> activity();
	
	/**
	 * 
	 * @return the calculated duration distance from the completion. The more is
	 *         this value, the more is the task priority
	 */

	long getCriticalDistance();

	/**
	 * 
	 * @return true if the task is on the critical path
	 */

	boolean isOnCriticalPath();

	
	
	
	@SuppressWarnings("rawtypes")
	public static  Comparator<IScheduledActivity>   comparator = new Comparator<IScheduledActivity>() {
		/**
		 * 
		 * This comparator will be used for sorting activities. The longer is
		 * critical distance, the faster we need to schedule. As a tie-breaker
		 * we use first {@link IActivity#isOnCriticalPath} and then
		 * {@link IActivity#duration}
		 * 
		 * @param sa1
		 * @param sa2
		 * @return
		 */

		@Override
		public int compare(IScheduledActivity sa1, IScheduledActivity sa2) {

			long diff = sa1.getCriticalDistance() - sa2.getCriticalDistance();

			if (diff != 0)
				return diff > 0 ? -1 : 1;

			//	
			if (sa1.isOnCriticalPath() != sa2.isOnCriticalPath())
				return sa1.isOnCriticalPath() ? -1 : 1;

			diff = sa1.activity().duration() - sa2.activity().duration();

			if (diff != 0)
				return diff > 0 ? -1 : 1;

			return 0;
		}

	}; 
			
	/**
	 * 
	 * This comparator will be used for sorting activities. The longer is
	 * critical distance, the faster we need to schedule. As a tie-breaker
	 * we use first {@link IActivity#isOnCriticalPath} and then
	 * {@link IActivity#duration}
	 * 
	 * @param sa1
	 * @param sa2
	 * @return
	 */

//	@Override
//	public int compare(IScheduledActivity<? extends IActivity> sa1, IScheduledActivity<? extends IActivity> sa2) {
//
//		long diff = sa1.getCriticalDistance() - sa2.getCriticalDistance();
//
//		if (diff != 0)
//			return diff > 0 ? -1 : 1;
//
//		//	
//		if (sa1.isOnCriticalPath() != sa2.isOnCriticalPath())
//			return sa1.isOnCriticalPath() ? -1 : 1;
//
//		diff = sa1.activity().duration() - sa2.activity().duration();
//
//		if (diff != 0)
//			return diff > 0 ? -1 : 1;
//
//		return 0;
//	}
			
			
			//new Comparator<IScheduledActivity<T , A extends IActivity<T>>>() {


} 
