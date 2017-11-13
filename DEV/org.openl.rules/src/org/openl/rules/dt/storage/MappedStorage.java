package org.openl.rules.dt.storage;


public abstract class MappedStorage extends ReadOnlyStorage<Object> {

	private Object[] uniqueValues;
	
	public MappedStorage(Object[] uniqueValues, StorageInfo info) {
		super(info);
		this.uniqueValues = uniqueValues;
	}
	
	@Override
	public Object getValue(int index) {
		return uniqueValues[mapIndex(index)];
	}
	
	protected abstract int mapIndex(int index); 

	@Override
	public boolean isSpace(int index) {
		return uniqueValues[mapIndex(index)] == null;
	}
	
	@Override
	public boolean isFormula(int index) {
		return  StorageUtils.isFormula(uniqueValues[mapIndex(index)]);
	}
	
	@Override
	public boolean isElse(int index) {
		return uniqueValues[mapIndex(index)] == IStorage.StorageType.ELSE;
	}
	
	/**
	 * 
	 * @param map  integer map array containing indexes in uniqueValues. The indexes may contain only values from 0 to uniqueValues.length - 1 
	 * @param uniqueValues array of stored objects
	 * @return
	 */
	public static IStorage<Object> makeNewStorage(int[] map,
			Object[] uniqueValues, StorageInfo info) {
		
		int mapMaxValue = uniqueValues.length - 1; 
		
		if (mapMaxValue <= Byte.MAX_VALUE)
			return new ByteMappedStorage(map, uniqueValues, info);
		
		if (mapMaxValue <= Byte.MAX_VALUE - Byte.MIN_VALUE)
			return new ByteExtMappedStorage(map, uniqueValues, info);
		
		if (mapMaxValue <= Short.MAX_VALUE)
			return new ShortMappedStorage(map, uniqueValues, info);
		
		if (mapMaxValue <= Short.MAX_VALUE - Short.MIN_VALUE)
			return new ShortExtMappedStorage(map, uniqueValues, info);
		
		return new IntMappedStorage(map, uniqueValues, info);
	}
	
	static class ByteMappedStorage extends MappedStorage {

		private byte[] bmap;
		
		public ByteMappedStorage(int[] map,  Object[] uniqueValues, StorageInfo info) {
			super(uniqueValues, info);
			initMap(map);
		}

		protected void initMap(int[] map) {
			int size = map.length;
			bmap = new byte[size];
			for (int i = 0; i < size; i++) {
				bmap[i] = (byte)map[i];
			}
		}

		@Override
		public final int size() {
			return bmap.length;
		}

		@Override
		protected int mapIndex(int index) {
			return bmap[index];
		}
	}
	
	static class ByteExtMappedStorage  extends MappedStorage {

		private byte[] bmap;
		
		public ByteExtMappedStorage(int[] map,  Object[] uniqueValues, StorageInfo info) {
			super(uniqueValues, info);
			initMap(map);
		}

		protected void initMap(int[] map) {
			int size = map.length;
			bmap = new byte[size];
			for (int i = 0; i < size; i++) {
				bmap[i] = (byte)(Byte.MAX_VALUE - map[i]);
			}
		}

		@Override
		public final int size() {
			return bmap.length;
		}

		@Override
		protected int mapIndex(int index) {
			return Byte.MAX_VALUE - bmap[index];
		}
	}
	

	
	static class ShortMappedStorage  extends MappedStorage {

		private short[] bmap;
		
		public ShortMappedStorage(int[] map,  Object[] uniqueValues, StorageInfo info) {
			super(uniqueValues, info);
			initMap(map);
		}

		protected void initMap(int[] map) {
			int size = map.length;
			bmap = new short[size];
			for (int i = 0; i < size; i++) {
				bmap[i] = (short)map[i];
			}
		}

		@Override
		public final int size() {
			return bmap.length;
		}

		@Override
		protected int mapIndex(int index) {
			return bmap[index];
		}
	}
	

	static class ShortExtMappedStorage  extends MappedStorage {

		private short[] bmap;
		
		public ShortExtMappedStorage(int[] map,  Object[] uniqueValues, StorageInfo info) {
			super(uniqueValues, info);
			initMap(map);
		}

		protected void initMap(int[] map) {
			int size = map.length;
			bmap = new short[size];
			for (int i = 0; i < size; i++) {
				bmap[i] = (short)(Short.MAX_VALUE - map[i]);
			}
		}

		@Override
		public final int size() {
			return bmap.length;
		}

		@Override
		protected int mapIndex(int index) {
			return Short.MAX_VALUE - bmap[index];
		}
	}
	
	
	static class IntMappedStorage  extends MappedStorage {

		private int[] map;
		
		public IntMappedStorage(int[] map,  Object[] uniqueValues, StorageInfo info) {
			super(uniqueValues, info);
			this.map = map;
		}

		@Override
		public final int size() {
			return map.length;
		}

		@Override
		protected int mapIndex(int index) {
			return map[index];
		}
		
		
	}
	
	
}
