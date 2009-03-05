package org.openl.rules.calc;

public abstract class SCellArray {
    public abstract SCell get(int i);

    public Object index(int i) {
        return get(i);
    }
}
