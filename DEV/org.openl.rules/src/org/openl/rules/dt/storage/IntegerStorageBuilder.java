package org.openl.rules.dt.storage;

import java.util.ArrayList;
import java.util.List;

public class IntegerStorageBuilder extends StorageBuilder {
	
	
	IIntStorage intStorage; 
			
	
	public IntegerStorageBuilder(int size){ 	
			intStorage = new IntegerStorage(size);
	}	
	
	
	Long min = null, max = null;
	List<Object> formulas = new ArrayList<Object>();
	

	@Override
	public void writeValue(Object value, int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeSpace(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeElse(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeFormula(Object formula, int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IStorage optimizeAndBuild() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void checkMinMax(Object loadedValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
