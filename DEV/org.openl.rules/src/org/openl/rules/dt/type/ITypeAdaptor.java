package org.openl.rules.dt.type;

import java.util.Date;

public interface ITypeAdaptor<T, C> {
	
	
	public C convert(T param);
	
	boolean supportsIncrement();
	
	C increment(C value);
	
	C getMinBound();
	C getMaxBound();
	
	
	
	static abstract class NumberTypeAdaptor<N extends Number> implements ITypeAdaptor<N, N>
	{
		public N convert(N param){return param;}

		@Override
		public boolean supportsIncrement() {
			return true;
		}
		
		
	}
	
	
	static public ITypeAdaptor<Integer, Integer>  INT = new NumberTypeAdaptor<Integer>() {

		@Override
		public Integer increment(Integer value) {
			return value + 1;
		}

		@Override
		public Integer getMinBound() {
			return Integer.MIN_VALUE + 1;
		}

		@Override
		public Integer getMaxBound() {
			return Integer.MAX_VALUE - 1;
		}
	}; 
	

	static public ITypeAdaptor<Date, Integer>  DATE  = new ITypeAdaptor<Date, Integer>(){

		static final long MS_IN_A_DAY = 1000 * 3600 * 24;
		
		@Override
		public Integer convert(Date date) {
			
			return (int)(date.getTime() / MS_IN_A_DAY);
		}

		@Override
		public boolean supportsIncrement() {
			return true;
		}

		@Override
		public Integer increment(Integer value) {
			return value+1;
		}

		
		
		
		@Override
		public Integer getMinBound() {
			return Integer.MIN_VALUE + 1;
		}

		@Override
		public Integer getMaxBound() {
			return Integer.MAX_VALUE -1;
		}

	}; 
	
	
	

}
