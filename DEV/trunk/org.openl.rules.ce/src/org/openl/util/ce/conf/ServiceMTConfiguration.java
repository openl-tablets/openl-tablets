package org.openl.util.ce.conf;

import java.util.HashMap;
import java.util.Map;


public class ServiceMTConfiguration implements IServiceMTConfiguration {

	int parallelLevel = Runtime.getRuntime().availableProcessors();
	int errorLimit = 0;
//	long minSequenceLengthNs = 50000;
	long minSequenceLengthNs = 100 * 1000; //100 us
	
	
	private ComponentMTBean[] componentMTBeans; 
	public ComponentMTBean[] getComponentMTBeans() {
		return componentMTBeans;
	}


	public synchronized void setComponentMTBeans(ComponentMTBean[] componentMTBeans) {
		this.componentMTBeans = componentMTBeans;
		for (int i = 0; i < componentMTBeans.length; i++) {
			componentMap.put(componentMTBeans[i].getComponentId(), componentMTBeans[i]);
		}
		
	}


	private Map<String, ComponentMTBean> componentMap = new HashMap<String, ComponentMTBean>(); 
	

	
	
	public int getParallelLevel() {
		return parallelLevel;
	}
	
	
	public void setParallelLevel(int parallelLevel) {
		this.parallelLevel = parallelLevel;
	}
	public int getErrorLimit() {
		return errorLimit;
	}
	public void setErrorLimit(int errorLimit) {
		this.errorLimit = errorLimit;
	}
	public long getMinSequenceLengthNs() {
		return minSequenceLengthNs;
	}
	public void setMinSequenceLengthNs(long minSequenceLengthNs) {
		this.minSequenceLengthNs = minSequenceLengthNs;
	}
	
	public boolean isCallComponentUsingMT(String uid) {
		ComponentMTBean bean = componentMap.get(uid);
		return bean != null ? bean.isCallComponentUsingMT() : false;
	}
	
	public boolean isExecuteComponentUsingMT(String uid) {
		ComponentMTBean bean = componentMap.get(uid);
		return bean != null ? bean.isExecuteComponentUsingMT() : false;
	}
	
	public long getComponentLengthNs(String uid) {
	
		ComponentMTBean bean = componentMap.get(uid);
		return bean != null ? bean.getComponentLength() : 0L;
	}

	

	
//	
	
	public synchronized void setCallComponentUsingMT(String uid, boolean isUsingMT) {
		ComponentMTBean bean = componentMap.get(uid);
		if (bean == null)
		{	
			bean = new ComponentMTBean(uid, getDefaultRunningComponentLength(), isUsingMT);
			componentMap.put(uid, bean);
		}
		else
			bean.setCallComponentUsingMT(isUsingMT);
	}

	public synchronized void setExecuteComponentUsingMT(String uid, boolean isExecuteComponentUsingMT) {
		ComponentMTBean bean = componentMap.get(uid);
		if (bean == null)
		{	
			bean = new ComponentMTBean(uid, getDefaultRunningComponentLength(), false, isExecuteComponentUsingMT);
			componentMap.put(uid, bean);
		}
		else
			bean.setExecuteComponentUsingMT(isExecuteComponentUsingMT);
	}
	
	
	public void setComponentLengthNs(String uid, long length) {
		
		ComponentMTBean bean = componentMap.get(uid);
		if (bean == null)
		{	
			bean = new ComponentMTBean(uid, length, true);
			componentMap.put(uid, bean);
		}
		else
			bean.setComponentLength(length);
		
	}


	@Override
	public long getDefaultRunningComponentLength() {
		return getMinSequenceLengthNs();
	} 
	

}
