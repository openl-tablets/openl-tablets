package org.openl.rules.dt2.storage;

import static org.openl.rules.dt2.storage.IStorage.StorageType.ELSE;
import static org.openl.rules.dt2.storage.IStorage.StorageType.SPACE;
import static org.openl.rules.dt2.storage.StorageUtils.isFormula;

import java.util.Map;

public abstract class StorageBuilder<T> {
	
	
	StorageInfo info = new StorageInfo();
	
	
//	private static final int MIN_CHECK_INDEX = 1000;
//	private static final double MAX_DIFF_RATIO = 0.7;
	
	
	
	public abstract void writeValue(T value, int index);
	public abstract void writeSpace(int index);
	public abstract void writeElse(int index);
	public abstract void writeFormula(Object formula, int index);

	public abstract IStorage<T> optimizeAndBuild();

	
	protected abstract void checkMinMax(Object loadedValue);
	
	
	@SuppressWarnings("unchecked")
	public void writeObject(Object loadedValue, int index) {
		if (loadedValue == null || loadedValue == SPACE)
		{
			writeSpace(index);
			info.addSpaceIndex(index);
		}	
		else if (loadedValue == ELSE)
		{
			writeElse(index);
			info.addElseIndex(index);
		}
		else if (isFormula(loadedValue))
		{
			writeFormula(loadedValue, index);
			info.addFormulaIndex(index);
		}	
		else
		{
			checkMinMax(loadedValue);
			checkDiffValues(loadedValue, index);
			writeValue((T)loadedValue, index);
		}	
	}
	
	
	
	protected int checkDiffValues(Object loadedValue, int currentIndex)
	{

		Map<Object, Integer> diffValues = info.getUniqueIndex();  
		
		Integer index = diffValues.get(loadedValue); 
		if (index != null)
			return index;
		
		
		
//		double diffSize = diffValues.size() + 1;
//		
//		//check if too many diffValues
//		
//		if (currentIndex > MIN_CHECK_INDEX &&  ((double)diffSize) / (currentIndex + 1) >  MAX_DIFF_RATIO)
//		{
//			diffValues = null;
//			return;
//		}	
		
		int size = diffValues.size();
		diffValues.put(loadedValue, size);
		return size;
		
	}

	public abstract int size();
	
	
	
	}
