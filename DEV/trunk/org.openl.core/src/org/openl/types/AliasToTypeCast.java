package org.openl.types;


public class AliasToTypeCast implements IOpenCast {

	private IOpenClass from;
	private IOpenClass to;
	
	public AliasToTypeCast(IOpenClass from, IOpenClass to) {
		this.from = from;
		this.to = to;
	}

	public Object convert(Object from) {
		
		return null;
	}

	public int getDistance(IOpenClass from, IOpenClass to) {
		return 0;
	}

	public boolean isImplicit() {
		return true;
	}

}
