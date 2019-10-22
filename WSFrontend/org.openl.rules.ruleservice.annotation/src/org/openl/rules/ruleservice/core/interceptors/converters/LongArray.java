package org.openl.rules.ruleservice.core.interceptors.converters;

import java.util.Collection;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LongArray {
    private Long[] array;

    public LongArray() {

    }

    public LongArray(Long[] array) {
        this.array = array;
    }

    public LongArray(Collection<Long> collection) {
        this.array = collection != null ? collection.toArray(new Long[collection.size()]) : new Long[0];
    }

    public void setArray(Long[] array) {
        this.array = array;
    }

    public Long[] getArray() {
        return array;
    }
}