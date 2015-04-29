package org.openl.domain;

import javax.xml.bind.annotation.XmlTransient;

public abstract class FixedSizeDomain<T> implements IFiniteDomain<T> {
    
    @XmlTransient
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