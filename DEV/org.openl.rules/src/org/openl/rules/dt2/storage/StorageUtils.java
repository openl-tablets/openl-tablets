package org.openl.rules.dt2.storage;

import org.openl.rules.dt2.element.ArrayHolder;
import org.openl.types.IOpenMethod;

public class StorageUtils {

	public static boolean isFormula(Object loadedValue) {
		return loadedValue != null && (loadedValue instanceof IOpenMethod || loadedValue instanceof ArrayHolder);
	}
	
	
}
