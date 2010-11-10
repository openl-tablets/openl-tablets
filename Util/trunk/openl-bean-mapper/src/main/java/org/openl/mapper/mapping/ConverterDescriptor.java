package org.openl.mapper.mapping;

import org.dozer.CustomConverter;

public class ConverterDescriptor {

    private String converterId;
    private CustomConverter instance;

    public ConverterDescriptor(String converterId, CustomConverter instance) {
        this.converterId = converterId;
        this.instance = instance;
    }

    public String getConverterId() {
        return converterId;
    }

    public CustomConverter getInstance() {
        return instance;
    }

}
