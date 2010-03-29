package org.openl.domain;

public abstract class FixedSizeDomain<T> implements IFiniteDomain<T> {

    final public boolean isFinite() {
        return true;
    }

    final public int maxSize() {
        return size();
    }

    final public int minSize() {
        return size();
    }

}