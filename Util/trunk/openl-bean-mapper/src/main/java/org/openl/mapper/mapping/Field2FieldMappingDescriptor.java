package org.openl.mapper.mapping;

public class Field2FieldMappingDescriptor {

    private String fieldA;
    private String fieldB;
    private ConverterDescriptor converter;

    public String getFieldA() {
        return fieldA;
    }

    public void setFieldA(String fieldA) {
        this.fieldA = fieldA;
    }

    public String getFieldB() {
        return fieldB;
    }

    public void setFieldB(String fieldB) {
        this.fieldB = fieldB;
    }

    public ConverterDescriptor getConverter() {
        return converter;
    }

    public void setConverter(ConverterDescriptor converter) {
        this.converter = converter;
    }
    
}
