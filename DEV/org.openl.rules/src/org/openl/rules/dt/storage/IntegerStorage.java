package org.openl.rules.dt.storage;

public class IntegerStorage implements IIntStorage {
	
	int[] array;
	
	
	
	public IntegerStorage(int size)
	{
		array = new int[size];
	}

	@Override
	public Byte getByteValue(int index) {
		return Byte.valueOf((byte)array[index]);
	}

	@Override
	public Integer getIntegerValue(int index) {
		return  Integer.valueOf(array[index]);
	}

	@Override
	public Short getShortValue(int index) {
		return Short.valueOf((short)array[index]);
	}

	@Override
	public Long getLongValue(int index) {
		return Long.valueOf(array[index]);

	}

	@Override
	public Character getCharacterValue(int index) {
		return Character.valueOf((char)array[index]);
	}

	@Override
	public void setByteValue(int index, Byte b) {
		array[index] = b;
	}

	@Override
	public void setIntegerValue(int index, Integer i) {
		array[index] = i;
		
	}

	@Override
	public void setShortValue(int index, Short s) {
		array[index] = s;
	}

	@Override
	public void setLongValue(int index, Long l) {
		array[index] = l.intValue();
		
	}

	@Override
	public void setCharacterValue(int index, Character c) {
		array[index] = c;
		
	}
	
	@Override
	public Object getNativeValue(int index) {
		return array[index];
	}

	@Override
	public void setNativeValue(int index, Object x) {
		array[index] = (Integer)x;
	}

	@Override
	public int size() {
		return array.length;
	}
	

}
