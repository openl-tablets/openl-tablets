package org.openl.rules.dt2.storage;

public abstract class ReadOnlyStorage<T> implements IStorage<T> {



	
	
	@Override
	public void setValue(int index, Object o) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void setSpace(int index) {
		throw new UnsupportedOperationException();
		
	}
	@Override
	public void setElse(int index) {
		throw new UnsupportedOperationException();
		
	}
	@Override
	public void setFormula(int index, Object formula) {
		throw new UnsupportedOperationException();
		
	}

}
