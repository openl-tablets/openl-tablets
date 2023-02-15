package org.openl.rules.dt.storage;

import org.openl.rules.dt.Expr;

public abstract class ReadOnlyStorage<T> implements IStorage<T> {

    private final StorageInfo info;

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

    @Override
    public Expr getExprValue(int index) {
        return null;
    }

    @Override
    public StorageInfo getInfo() {
        return info;
    }
}
