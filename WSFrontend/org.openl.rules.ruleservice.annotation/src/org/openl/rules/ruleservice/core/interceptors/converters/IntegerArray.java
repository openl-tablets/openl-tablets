package org.openl.rules.ruleservice.core.interceptors.converters;

import java.util.Collection;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IntegerArray {
    private Integer[] array;

    public IntegerArray() {

    }

    public IntegerArray(Integer[] array) {
        this.array = array;
    }

    public IntegerArray(Collection<Integer> collection) {
        this.array = collection != null ? collection.toArray(new Integer[collection.size()]) : new Integer[0];
    }

    public void setArray(Integer[] array) {
        this.array = array;
    }

    public Integer[] getArray() {
        return array;
    }
}