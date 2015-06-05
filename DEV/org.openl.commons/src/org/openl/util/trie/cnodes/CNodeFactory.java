package org.openl.util.trie.cnodes;



public class CNodeFactory {
	
	
	public static int arraySize(Class<?> arrayClass, int size)
	{
		if (arrayClass == byte[].class)
		{
			return 16 + 8 * ((size + 7) / 8 ); 
		}
		if (arrayClass == short[].class)
		{
			return 16 + 8 * ((size + 3) / 4); 
		}
		
		if (arrayClass == char[].class)
		{
			return 16 + 8 * ((size + 3) / 4); 
		}
		if (arrayClass == int[].class)
		{
			return 16 + 8 * ((size + 1) / 2); 
		}

		if (arrayClass == double[].class)
		{
			return 16 + 8 * size; 
		}
		if (arrayClass == long[].class)
		{
			return 16 + 8 * size; 
		}
		
		if (arrayClass.isArray())
		{
			return 16 + 8 * ((size + 1) / 2); 
		}	
		
		throw new RuntimeException("Unknown array type " + arrayClass.getName());
		
	}
	

	
	public static void main(String[] args) {
		check(byte[].class, 8);
		check(byte[].class, 9);
		check(byte[].class, 33);
		check(short[].class, 0);
		check(short[].class, 4);
		check(short[].class, 5);
		check(int[].class, 0);
		check(int[].class, 4);
		check(int[].class, 5);
		check(int[].class, 6);
	}



	private static void check(Class<?> aclass, int asize) {
		int size = arraySize(aclass, asize);
		
		System.out.println(aclass.getCanonicalName() + "[" + asize + "] = " + size );
		
	}



	public static boolean useMapper(int range, Class<?> mapperClass, int count,
			Class<?> valueClass) {
		
		int mapperSize = arraySize(mapperClass, range) + arraySize(valueClass, count); 
		int directSize = arraySize(valueClass, range); 
		
		return mapperSize * 10  < directSize * 9;
	}
	
	
	
	
	
	
	
}
