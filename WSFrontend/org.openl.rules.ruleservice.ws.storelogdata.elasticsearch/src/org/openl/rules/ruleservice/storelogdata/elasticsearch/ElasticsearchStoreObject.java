package org.openl.rules.ruleservice.storelogdata.elasticsearch;

/**
 * Wrapper for storing the plain string values as searchable objects in elasticsearch
 */
public class ElasticsearchStoreObject {
    private String value;

    public ElasticsearchStoreObject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
