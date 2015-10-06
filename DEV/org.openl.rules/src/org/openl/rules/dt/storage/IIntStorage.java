package org.openl.rules.dt.storage;

public interface IIntStorage  {

	Byte getByteValue(int index);
	Integer getIntegerValue(int index);
	Short getShortValue(int index);
	Long getLongValue(int index);
	Character getCharacterValue(int index);
	
	Object getNativeValue(int index);
	
	void setByteValue(int index, Byte b);
	void setIntegerValue(int index, Integer i);
	void setShortValue(int index, Short s);
	void setLongValue(int index, Long l);
	void setCharacterValue(int index, Character c);
	
	void setNativeValue(int index, Object x);
	int size();
}
