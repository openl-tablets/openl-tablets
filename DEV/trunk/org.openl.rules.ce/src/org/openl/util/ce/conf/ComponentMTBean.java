package org.openl.util.ce.conf;




public class ComponentMTBean {

	private String componentId;
	private long componentLength;
	private boolean callComponentUsingMT;
	private boolean executeComponentUsingMT;
	
	
	public boolean isExecuteComponentUsingMT() {
		return executeComponentUsingMT;
	}

	public void setExecuteComponentUsingMT(boolean executeComponentUsingMT) {
		this.executeComponentUsingMT = executeComponentUsingMT;
	}

	public boolean isCallComponentUsingMT() {
		return callComponentUsingMT;
	}

	public void setCallComponentUsingMT(boolean callComponentUsingMT) {
		this.callComponentUsingMT = callComponentUsingMT;
	}

	public ComponentMTBean(String componentId, long componentLength,
			boolean callComponentUsingMT) {
		super();
		this.componentId = componentId;
		this.componentLength = componentLength;
		this.callComponentUsingMT = callComponentUsingMT;
	}

	public ComponentMTBean(String componentId, long componentLength,
			boolean callComponentUsingMT, boolean executeComponentUsingMT) {
		super();
		this.componentId = componentId;
		this.componentLength = componentLength;
		this.callComponentUsingMT = callComponentUsingMT;
		this.executeComponentUsingMT = executeComponentUsingMT;
	}

	public ComponentMTBean() {}
	
	public String getComponentId() {
		return componentId;
	}
	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	public long getComponentLength() {
		return componentLength;
	}
	public void setComponentLength(long componentLength) {
		this.componentLength = componentLength;
	}

	@Override
	public String toString() {
		return "ComponentMTBean [componentId=" + componentId
				+ ", componentLength=" + componentLength
				+ ", isComponentUsingMT=" + callComponentUsingMT + "]";
	}
	
	
}
