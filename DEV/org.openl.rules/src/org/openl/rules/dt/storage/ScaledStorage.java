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

    public int size() {
        return s.size() * scale.getMultiplier();
    }

    public Object getValue(int index) {
        return s.getValue(actualIndex(index));
    }

    public boolean isSpace(int index) {
        return s.isSpace(actualIndex(index));
    }

    public boolean isFormula(int index) {
        return s.isFormula(actualIndex(index));
    }

    public boolean isElse(int index) {
        return s.isElse(actualIndex(index));
    }

    private int actualIndex(int index) {
        return scale.getActualIndex(index);
    }

}
