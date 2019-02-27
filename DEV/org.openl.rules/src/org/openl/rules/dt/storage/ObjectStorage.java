package org.openl.rules.dt.storage;

import static org.openl.rules.dt.storage.IStorage.StorageType.ELSE;

public class ObjectStorage implements IStorage<Object> {

    private Object[] values;
    private StorageInfo info;

    ObjectStorage(int size) {
        values = new Object[size];
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public Object getValue(int index) {
        return values[index];
    }

    @Override
    public boolean isSpace(int index) {
        return values[index] == null || values[index] == StorageType.SPACE;
    }

    @Override
    public boolean isFormula(int index) {
        return StorageUtils.isFormula(values[index]);
    }

    @Override
    public boolean isElse(int index) {
        return values[index] == ELSE;
    }

    @Override
    public void setValue(int index, Object o) {
        values[index] = o;
    }

    @Override
    public void setSpace(int index) {
        values[index] = null; // TODO SPACE?

    }

    @Override
    public void setElse(int index) {
        values[index] = ELSE;
    }

    @Override
    public void setFormula(int index, Object formula) {
        values[index] = formula;
    }

    public StorageInfo getInfo() {
        return info;
    }

    public void setInfo(StorageInfo info) {
        this.info = info;
    }

    public Object[] getValues() {
        return values;
    }

}
