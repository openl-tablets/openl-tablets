package org.openl.rules.dt.storage;

import static org.openl.rules.dt.storage.IStorage.StorageType.ELSE;
import static org.openl.rules.dt.storage.IStorage.StorageType.SPACE;
import static org.openl.rules.dt.storage.StorageUtils.isFormula;

import java.util.Map;

public abstract class StorageBuilder<T> implements IStorageBuilder<T> {

    StorageInfo info = new StorageInfo();

    public abstract void writeValue(T value, int index);

    public abstract void writeSpace(int index);

    public abstract void writeElse(int index);

    public abstract void writeFormula(Object formula, int index);

    @Override
    public abstract IStorage<T> optimizeAndBuild();

    protected abstract void checkMinMax(Object loadedValue);

    @Override
    @SuppressWarnings("unchecked")
    public void writeObject(Object loadedValue, int index) {
        if (loadedValue == null || loadedValue == SPACE) {
            writeSpace(index);
            info.addSpaceIndex();
        } else if (loadedValue == ELSE) {
            writeElse(index);
            info.addElseIndex();
        } else if (isFormula(loadedValue)) {
            writeFormula(loadedValue, index);
            info.addFormulaIndex();
        } else {
            checkMinMax(loadedValue);

            Map<Object, Integer> diffValues = info.getUniqueIndex();

            Integer index1 = diffValues.get(loadedValue);
            if (index1 == null) {
                int size = diffValues.size();
                diffValues.put(loadedValue, size);
            }

            writeValue((T) loadedValue, index);
        }
    }

    @Override
    public abstract int size();

    public StorageInfo getInfo() {
        return info;
    }

}
