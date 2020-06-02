package org.openl.rules.webstudio.util.converter.impl;

import java.util.List;

public class DatatypeDto {
    private String name;
    private List<FieldDto> fields;

    public DatatypeDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FieldDto> getFields() {
        return fields;
    }

    public void setFields(List<FieldDto> fields) {
        this.fields = fields;
    }
}
