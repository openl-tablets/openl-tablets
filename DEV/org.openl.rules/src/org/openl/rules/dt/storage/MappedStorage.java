package org.openl.rules.dt.storage;

import org.openl.rules.dt.Expr;

abstract class MappedStorage extends ReadOnlyStorage<Object> {

    private final Object[] uniqueValues;
    private IStorage storage;

    MappedStorage(Object[] uniqueValues, IStorage storage, StorageInfo info) {
        super(info);
        this.uniqueValues = uniqueValues;
        this.storage = storage;
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
        return StorageUtils.isFormula(uniqueValues[mapIndex(index)]);
    }

    @Override
    public boolean isElse(int index) {
        return uniqueValues[mapIndex(index)] == IStorage.StorageType.ELSE;
    }

    @Override
    public Expr getExprValue(int index) {
        return storage.getExprValue(index);
    }

    @Override
    public void removeExprs() {
        this.storage = null;
    }
}
