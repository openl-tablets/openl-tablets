package org.openl.rules.dt.storage;

class EmptyStorage implements IStorage<Object> {

    private final StorageInfo info;

    EmptyStorage(StorageInfo info) {
        this.info = info;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public StorageInfo getInfo() {
        return info;
    }

    @Override
    public Object getValue(int index) {
        return null;
    }

    @Override
    public boolean isSpace(int index) {
        return true;
    }

    @Override
    public boolean isFormula(int index) {
        return false;
    }

    @Override
    public boolean isElse(int index) {
        return false;
    }

    @Override
    public void setValue(int index, Object o) {

    }

    @Override
    public void setSpace(int index) {

    }

    @Override
    public void setElse(int index) {

    }

    @Override
    public void setFormula(int index, Object formula) {

    }
}
