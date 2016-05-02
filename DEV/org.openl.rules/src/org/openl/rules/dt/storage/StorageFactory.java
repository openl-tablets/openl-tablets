package org.openl.rules.dt.storage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.rules.dt.DTScale;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

@SuppressWarnings("rawtypes")
public class StorageFactory {
	
	private static final Class[] integerTypes = {int.class, Integer.class,  byte.class, Byte.class, char.class, Character.class, short.class, Short.class, long.class, Long.class, BigInteger.class, Date.class};
	private static final Class[] decimalTypes = {double.class, Double.class,  float.class, Float.class, BigDecimal.class};

	public IStorage makeStorage(JavaOpenClass type, int size, Object min, Object max, int decimalPlaces, int nSpaces, int nElses, int nFormulas)
	{
		
		
		JavaOpenClass type2 = JavaOpenClass.OBJECT;
		if (isInteger(type))
			type2 = selectInteger(min, max, nSpaces + nElses + nFormulas);
		return null;
		
		
	}

	private JavaOpenClass selectInteger(Object min, Object max, int nSpecialCases) {
		
		
		
		return null;
	}

	private boolean isInteger(JavaOpenClass type) {
		Class<?> c = type.getInstanceClass();
		
		return ArrayUtils.contains(integerTypes, c) ;
	}


	public static IStorageBuilder makeStorageBuilder(
			IParameterDeclaration iParameterDeclaration, int size, DTScale.RowScale scale) {
		
		int newSize = scale.getActualSize(size);
		
		StorageBuilder sb =  makeStorageBuilder(iParameterDeclaration, newSize);
		
		if (newSize == size)
			return sb;
		
		return new ScaleStorageBuilder(scale, sb);
	}
	
	
	private static StorageBuilder makeStorageBuilder(
			IParameterDeclaration iParameterDeclaration, int size) {
		return new ObjectStorageBuilder(size);
	}

}
