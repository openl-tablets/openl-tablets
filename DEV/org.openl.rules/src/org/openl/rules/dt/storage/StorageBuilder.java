package org.openl.rules.dt.storage;

import static org.openl.rules.dt.storage.IStorage.StorageType.ELSE;
import static org.openl.rules.dt.storage.IStorage.StorageType.SPACE;
import static org.openl.rules.dt.storage.StorageUtils.isFormula;

import java.util.Map;

import lombok.Getter;

public abstract class StorageBuilder<T> implements IStorageBuilder<T> {

    @Getter
    final StorageInfo info = new StorageInfo();

    public abstract void writeValue(T value, int index);

    public abstract void writeSpace(int index);

    public abstract void writeElse(int index);

    public abstract void writeFormula(Object formula, int index);

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
            diffValues.putIfAbsent(loadedValue, diffValues.size());

            writeValue((T) loadedValue, index);
        }
    }

}
