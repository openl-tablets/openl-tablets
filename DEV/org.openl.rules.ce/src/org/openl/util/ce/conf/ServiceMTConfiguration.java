package org.openl.util.ce.conf;

import java.util.HashMap;
import java.util.Map;

import org.openl.util.Log;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServiceMTConfiguration  {

	public ServiceMTConfiguration() {
	}

	public static ServiceMTConfiguration loadProjectResolverFromClassPath() {
		try {
			ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
					"rules-ce-conf.xml");
			ServiceMTConfiguration conf = (ServiceMTConfiguration) applicationContext
					.getBean("rules-ce-conf");
			return conf;
		} catch (Throwable t) {
			Log.error("Error loading rules-ce-conf.xml from classpath", t);
			return new ServiceMTConfiguration();
		}
	}

	int totalParallelLevel = Runtime.getRuntime().availableProcessors();
	int errorLimit = 0;
	// long minSequenceLengthNs = 50000;
	long minSequenceLengthNs = 100 * 1000; // 100 us

	private ComponentMTBean[] componentMTBeans;

	public ComponentMTBean[] getComponentMTBeans() {
		return componentMTBeans;
	}

	public synchronized void setComponentMTBeans(
			ComponentMTBean[] componentMTBeans) {
		this.componentMTBeans = componentMTBeans;
		for (int i = 0; i < componentMTBeans.length; i++) {
			componentMap.put(componentMTBeans[i].getComponentId(),
					componentMTBeans[i]);
		}

	}

	private Map<String, ComponentMTBean> componentMap = new HashMap<String, ComponentMTBean>();
	private int maxPerRequestParallelLevel;

	public int getTotalParallelLevel() {
		return totalParallelLevel;
	}

	public void setParallelLevel(int parallelLevel) {
		this.totalParallelLevel = parallelLevel;
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

	public synchronized void setCallComponentUsingMT(String uid, long runLengthNs,
			boolean isUsingMT) {
		ComponentMTBean bean = componentMap.get(uid);
		
		if ( runLengthNs == 0)
			runLengthNs = getDefaultRunningComponentLength();
		
		if (bean == null) {
			bean = new ComponentMTBean(uid, runLengthNs,
					isUsingMT);
			componentMap.put(uid, bean);
		} 
		else
		{	
			bean.setCallComponentUsingMT(isUsingMT);
			bean.setComponentLength(runLengthNs);
		}	
	}

	public synchronized void setExecuteComponentUsingMT(String uid, long runLengthNs,
			boolean isExecuteComponentUsingMT) {
		ComponentMTBean bean = componentMap.get(uid);
		if ( runLengthNs == 0)
			runLengthNs = getDefaultRunningComponentLength();
		if (bean == null) {
			bean = new ComponentMTBean(uid, runLengthNs,
					false, isExecuteComponentUsingMT);
			componentMap.put(uid, bean);
		} else
		{
			bean.setExecuteComponentUsingMT(isExecuteComponentUsingMT);
			bean.setComponentLength(runLengthNs);
		}
	}

	public void setComponentLengthNs(String uid, long length) {

		ComponentMTBean bean = componentMap.get(uid);
		if (bean == null) {
			bean = new ComponentMTBean(uid, length, true);
			componentMap.put(uid, bean);
		} else
			bean.setComponentLength(length);

	}

	public long getDefaultRunningComponentLength() {
		return getMinSequenceLengthNs();
	}

	
	public int getMaxPerRequestParallelLevel() {
		return maxPerRequestParallelLevel == 0 ? getTotalParallelLevel()
				: maxPerRequestParallelLevel;
	}

	public void setMaxPerRequestParallelLevel(int maxPerRequestParallelLevel) {
		this.maxPerRequestParallelLevel = maxPerRequestParallelLevel;
	}

}
