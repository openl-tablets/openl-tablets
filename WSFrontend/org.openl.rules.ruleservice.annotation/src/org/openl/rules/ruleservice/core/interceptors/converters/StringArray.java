package org.openl.rules.ruleservice.core.interceptors.converters;

import java.util.Collection;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StringArray {
    private String[] array;

    public StringArray() {

    }

    public StringArray(String[] array) {
        this.array = array;
    }

    public StringArray(Collection<String> collection) {
        this.array = collection != null ? collection.toArray(new String[collection.size()]) : new String[0];
    }

    public void setArray(String[] array) {
        this.array = array;
    }

    public String[] getArray() {
        return array;
    }
}