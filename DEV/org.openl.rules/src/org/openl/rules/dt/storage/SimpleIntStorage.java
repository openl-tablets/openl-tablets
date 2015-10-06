package org.openl.rules.dt.storage;


@SuppressWarnings("rawtypes")
public class SimpleIntStorage implements IStorage {
	
	StorageInfo info;
	
	static interface Convertor<From,To>
	{
		To convertWrite(From from );
		From convertRead(To to);
	}
	
	
	static final Convertor<Byte, Integer> Byte2Integer = new Convertor<Byte, Integer>(){


		@Override
		public Integer convertWrite(Byte from) {
			return from.intValue();
		}

		@Override
		public Byte convertRead(Integer to) {
			return to.byteValue();
		}}; 


	static final Convertor<Short, Integer> Short2Integer = new Convertor<Short, Integer>(){

		@Override
		public Integer convertWrite(Short from) {
			return from.intValue();
		}

		@Override
		public Short convertRead(Integer to) {
			return to.shortValue();
		}
		
	};

	
	Convertor convertor;
	IIntStorage storage;
	
	
	

	@Override
	public int size() {
		return storage.size();
	}


	@Override
	public Object getValue(int index) {
		return storage.getNativeValue(index);
	}


	@Override
	public boolean isSpace(int index) {
		return false;
	}


	@Override
	public boolean isFormula(int index) {
		return false;
	}


	@Override
	public boolean isElse(int index) {
		return false;
	}


	@SuppressWarnings("unchecked")
	@Override
	public void setValue(int index, Object o) {
		storage.setNativeValue(index, convertor.convertWrite(o));
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


	public StorageInfo getInfo() {
		return info;
	}


	public void setInfo(StorageInfo info) {
		this.info = info;
	}

}
