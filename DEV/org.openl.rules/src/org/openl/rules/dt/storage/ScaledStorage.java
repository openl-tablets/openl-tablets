package org.openl.rules.dt.storage;

import org.openl.rules.dt.DTScale.RowScale;

public class ScaledStorage extends ReadOnlyStorage {

    private RowScale scale;
    private IStorage s;

    ScaledStorage(RowScale scale, IStorage s, StorageInfo info) {
        super(info);
        this.scale = scale;
        this.s = s;
    }

    @Override
    public int size() {
        return s.size() * scale.getMultiplier();
    }

    @Override
    public Object getValue(int index) {
        return s.getValue(actualIndex(index));
    }

    @Override
    public boolean isSpace(int index) {
        return s.isSpace(actualIndex(index));
    }

    @Override
    public boolean isFormula(int index) {
        return s.isFormula(actualIndex(index));
    }

    @Override
    public boolean isElse(int index) {
        return s.isElse(actualIndex(index));
    }

    private int actualIndex(int index) {
        return scale.getActualIndex(index);
    }

}
