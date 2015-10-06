package org.openl.rules.dt.storage;

public class LongStorage implements IIntStorage {
	
	long[] array;
	
	void init(int size)
	{
		array = new long[size];
	}

	@Override
	public Byte getByteValue(int index) {
		return Byte.valueOf((byte)array[index]);
	}

	@Override
	public Integer getIntegerValue(int index) {
		return  Integer.valueOf((int)array[index]);
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
		array[index] = l;
		
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
		array[index] = (Long)x;
	}

	@Override
	public int size() {
		return array.length;
	}
	
}
