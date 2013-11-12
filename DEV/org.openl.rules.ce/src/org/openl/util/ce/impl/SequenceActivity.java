package org.openl.util.ce.impl;

import java.util.ArrayList;
import java.util.List;

import org.openl.util.ce.IActivity;
import org.openl.util.ce.ICallableActivity;
import org.openl.util.ce.IInvokableActivity;
import org.openl.util.ce.IScheduledActivity;
import org.openl.vm.IRuntimeEnv;

public class SequenceActivity implements IScheduledActivity {

	int id;
	int precedentSize;

	List<IScheduledActivity> dependents = new ArrayList<IScheduledActivity>();
	IScheduledActivity[] activities;
	boolean isInvokable;

	public SequenceActivity(IScheduledActivity[] activities, boolean isInvokable) {
		this.activities = activities;
		this.isInvokable = isInvokable;

	}

	@Override
	public IActivity activity() {
		if (isInvokable)
			return invokableActivity();
		return callableActivity();
	}

	private ICallableActivity<?> callableActivity() {
		return new ICallableActivity<Object>() {

			@Override
			public List<IActivity> dependsOn() {
				return null;
			}

			@Override
			public long duration() {
				return 0;
			}


			@Override
			public Object call() throws Exception {
				for (int i = 0; i < activities.length; i++) {
					((ICallableActivity<?>) activities[i].activity()).call();
				}

				return null;
			}
		};
	}

	public IInvokableActivity invokableActivity() {
		return new IInvokableActivity() {

			@Override
			public List<IActivity> dependsOn() {
				return null;
			}

			@Override
			public long duration() {
				return 0;
			}

			@Override
			public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
				for (int i = 0; i < activities.length; i++) {
					((IInvokableActivity) activities[i].activity()).invoke(
							target, params, env);
				}

				return null;
			}
		};

	}

	@Override
	public long getCriticalDistance() {
		return 0;
	}

	@Override
	public boolean isOnCriticalPath() {
		return false;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<IScheduledActivity> getDependents() {
		return dependents;
	}

	public void setDependents(List<IScheduledActivity> dependents) {
		this.dependents = dependents;
	}

	public int getPrecedentSize() {
		return precedentSize;
	}

	public void setPrecedentSize(int precedentSize) {
		this.precedentSize = precedentSize;
	}

}
