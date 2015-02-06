package org.openl.rules.dt2.storage;


public class MappedStorage implements IStorage<Object> {

	int[] map;
	Object[] uniqueValues;
	
	
	
	public MappedStorage(int[] map, Object[] uniqueValues) {
		super();
		this.map = map;
		this.uniqueValues = uniqueValues;
	}
	@Override
	public int size() {
		return map.length;
	}
	@Override
	public Object getValue(int index) {
		return uniqueValues[map[index]];
	}
	@Override
	public boolean isSpace(int index) {
		return uniqueValues[map[index]] == null;
	}
	
	@Override
	public boolean isFormula(int index) {
		
		return  StorageUtils.isFormula(uniqueValues[map[index]]);
	}
	
	@Override
	public boolean isElse(int index) {
		return uniqueValues[map[index]] == IStorage.StorageType.ELSE;
	}
	
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
