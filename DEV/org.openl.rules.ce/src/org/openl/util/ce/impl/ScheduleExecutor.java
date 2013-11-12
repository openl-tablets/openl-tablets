package org.openl.util.ce.impl;

import java.util.List;
import java.util.concurrent.Semaphore;

import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.ce.IScheduledActivity;
import org.openl.util.ce.InvokeFactory;

public class ScheduleExecutor {

	IScheduledActivity[] orderedActivities;
	int[][] dependents;
//	TaskExecutor[] executors;
//	InvokeFactory invokeFactory;

	public ScheduleExecutor(IScheduledActivity[] orderedActivities) {
		this.orderedActivities = orderedActivities;
//		this.invokeFactory = factory;
		prepareExecution();
	}

	protected void prepareExecution() {

		dependents = new int[orderedActivities.length][];
		for (int i = 0; i < orderedActivities.length; i++) {
			List<IScheduledActivity> dd = orderedActivities[i].getDependents();
			int size = dd == null ? 0 : dd.size();
			int[] depids = new int[size];
			for (int j = 0; j < depids.length; j++) {
				depids[j] = dd.get(j).getId();
			}

			dependents[i] = depids;
		}
//		executors = createExecutors();

	}

//	TreeSet<Integer> locked = new TreeSet<Integer>();
//
//	synchronized void acq(int id) {
//		locked.add(id);
//		log("Locked: " + locked);
//	}
//
//	synchronized void after(int id) {
//		locked.remove(id);
//		log("After: -" + id + locked);
//	}
//
//	TreeSet<Integer> rel = new TreeSet<Integer>();
//
//	synchronized void rel(int id) {
//		rel.remove(id);
//		log("REL: -" + id + rel);
//	}
//
//	synchronized void cr(int id) {
//		rel.add(id);
//		log("CR: " + rel);
//	}

//	synchronized void log(String string) {
//		System.out.println(string);
//
//	}

	public void execute(InvokeFactory factory) {
		
		TaskExecutor[] executors = createExecutors(factory);

		ServiceMT.getService().executeAll(executors);

	}

	private TaskExecutor[] createExecutors(InvokeFactory factory) {
		TaskExecutor[] exe = new TaskExecutor[orderedActivities.length];

		for (int i = 0; i < orderedActivities.length; i++) {
			exe[i] = new TaskExecutor(orderedActivities[i], exe, factory);
//			log("" + i + ".\t" + orderedActivities[i]);
//			cr(i);
		}
		return exe;
	}

	class TaskExecutor implements Runnable {

		IScheduledActivity act;
		Semaphore sem;
		TaskExecutor[] executors; 
		InvokeFactory invokeFactory;

		TaskExecutor(IScheduledActivity act, TaskExecutor[] executors, InvokeFactory factory) {
			this.act = act;
			this.executors = executors;
			this.invokeFactory = factory;
			int precedentSize = act.getPrecedentSize();
			sem = new Semaphore(-precedentSize + 1);
//			log("Created " + printDeps(act.getId()) + " sem = " + sem
//					+ " act = " + act);

		}

//		private String printDeps(int id) {
//			StringBuilder buf = new StringBuilder();
//			buf.append(id).append('[');
//			int[] deps = dependents[id];
//			for (int i = 0; i < deps.length; i++) {
//				if (i > 0)
//					buf.append(", ");
//				buf.append(deps[i]);
//			}
//			buf.append(']');
//			return buf.toString();
//		}

		@Override
		public void run() {
//			log("Acquiring " + act.getId() + " sem = " + sem + " act = " + act);
//			acq(act.getId());

			try {
				sem.acquire();
//				after(act.getId());
//				log("Acquired! " + act.getId() + " sem = " + sem + " act = "
//						+ act);

				invokeFactory.invoke(act.activity());
			} catch (Throwable t) {
				throw RuntimeExceptionWrapper.wrap(t);
			} finally {
				int[] deps = dependents[act.getId()];

//				rel(act.getId());
				for (int i = 0; i < deps.length; i++) {
//					log("releasing " + deps[i]);
					executors[deps[i]].release();
				}

			}

		}

		synchronized private void release() {
//			log("In Release " + act.getId() + " sem = " + sem + " act = " + act);

			sem.release();
		}

	}

}
