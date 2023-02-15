package org.openl.rules.dt.storage;

import org.openl.rules.dt.Expr;

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

    Expr getExprValue(int index);

    enum StorageType {
        VALUE,
        SPACE,
        ELSE,
        FORMULA
    }

}
