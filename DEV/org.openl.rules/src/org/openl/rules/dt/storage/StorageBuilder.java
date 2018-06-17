package org.openl.rules.dt.storage;

import static org.openl.rules.dt.storage.IStorage.StorageType.ELSE;
import static org.openl.rules.dt.storage.IStorage.StorageType.SPACE;
import static org.openl.rules.dt.storage.StorageUtils.isFormula;

import java.util.Map;

public abstract class StorageBuilder<T> implements IStorageBuilder<T> {
	
	
	StorageInfo info = new StorageInfo();
	
	
//	private static final int MIN_CHECK_INDEX = 1000;
//	private static final double MAX_DIFF_RATIO = 0.7;
	
	
	
	public abstract void writeValue(T value, int index);
	public abstract void writeSpace(int index);
	public abstract void writeElse(int index);
	public abstract void writeFormula(Object formula, int index);

	/* (non-Javadoc)
	 * @see org.openl.rules.dt2.storage.IStorageBuilder#optimizeAndBuild()
	 */
	@Override
	public abstract IStorage<T> optimizeAndBuild();

	
	protected abstract void checkMinMax(Object loadedValue);
	
	
	/* (non-Javadoc)
	 * @see org.openl.rules.dt2.storage.IStorageBuilder#writeObject(java.lang.Object, int)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void writeObject(Object loadedValue, int index) {
		if (loadedValue == null || loadedValue == SPACE)
		{
			writeSpace(index);
			info.addSpaceIndex();
		}	
		else if (loadedValue == ELSE)
		{
			writeElse(index);
			info.addElseIndex();
		}
		else if (isFormula(loadedValue))
		{
			writeFormula(loadedValue, index);
			info.addFormulaIndex();
		}	
		else
		{
			checkMinMax(loadedValue);
			checkDiffValues(loadedValue, index);
			writeValue((T)loadedValue, index);
		}	
	}
	
	
	
	protected void checkDiffValues(Object loadedValue, int currentIndex)
	{

		Map<Object, Integer> diffValues = info.getUniqueIndex();  
		
		Integer index = diffValues.get(loadedValue); 
		if (index != null)
			return;
		
		
		
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

	}

	/* (non-Javadoc)
	 * @see org.openl.rules.dt2.storage.IStorageBuilder#size()
	 */
	@Override
	public abstract int size();
	public StorageInfo getInfo() {
		return info;
	}
	
	
	
	}
