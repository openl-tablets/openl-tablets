package org.openl.rules.dt.storage;

import org.openl.rules.dt.element.ArrayHolder;
import org.openl.types.IOpenMethod;

public class StorageUtils {

	public static boolean isFormula(Object loadedValue) {
		return loadedValue != null && (loadedValue instanceof IOpenMethod || loadedValue instanceof ArrayHolder);
	}
	
	
}
