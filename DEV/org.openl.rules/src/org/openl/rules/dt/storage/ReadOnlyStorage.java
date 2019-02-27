package org.openl.rules.dt.storage;

public abstract class ReadOnlyStorage<T> implements IStorage<T> {

    private StorageInfo info;

    ReadOnlyStorage(StorageInfo info) {
        this.info = info;
    }

    @Override
    public void setValue(int index, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSpace(int index) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void setElse(int index) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void setFormula(int index, Object formula) {
        throw new UnsupportedOperationException();

    }

    public StorageInfo getInfo() {
        return info;
    }

    public void setInfo(StorageInfo info) {
        this.info = info;
    }

}
