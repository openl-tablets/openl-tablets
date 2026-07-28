package org.openl.rules.dt.storage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.dt.Expr;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class EmptyStorage implements IStorage<Object> {

    @Getter
    private final StorageInfo info;

    @Override
    public int size() {
        return 0;
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
    public Expr getExprValue(int index) {
        return null;
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

    @Override
    public void removeExprs() {
    }
}
