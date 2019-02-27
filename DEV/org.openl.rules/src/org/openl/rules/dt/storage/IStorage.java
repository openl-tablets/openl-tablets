package org.openl.rules.dt.storage;

public interface IStorage<T> {

    int size();

    StorageInfo getInfo();

    Object getValue(int index);

    boolean isSpace(int index);

    boolean isFormula(int index);

    boolean isElse(int index);

    void setValue(int index, Object o);

    void setSpace(int index);

    void setElse(int index);

    void setFormula(int index, Object formula);

    enum StorageType {
        VALUE,
        SPACE,
        ELSE,
        FORMULA;
    }

}
