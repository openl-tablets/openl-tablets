package org.openl.rules.dt.storage;

import org.openl.rules.dt.element.ArrayHolder;
import org.openl.types.IOpenMethod;

final class StorageUtils {

    private StorageUtils() {
    }

    static boolean isFormula(Object loadedValue) {
        return loadedValue instanceof IOpenMethod || loadedValue instanceof ArrayHolder;
    }
}
