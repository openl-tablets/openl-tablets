package org.openl.rules.dt.storage;

abstract class MappedStorage extends ReadOnlyStorage<Object> {

    private Object[] uniqueValues;

    MappedStorage(Object[] uniqueValues, StorageInfo info) {
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
        return StorageUtils.isFormula(uniqueValues[mapIndex(index)]);
    }

    @Override
    public boolean isElse(int index) {
        return uniqueValues[mapIndex(index)] == IStorage.StorageType.ELSE;
    }

}
