package org.openl.extension.xmlrules.model.single;

import java.util.List;

import org.openl.extension.xmlrules.model.DataInstance;
import org.openl.extension.xmlrules.model.Field;

public class DataInstanceImpl implements DataInstance {
    private String type;
    private String name;
    private List<Field> fields;
    private List<List<String>> values;

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public List<List<String>> getValues() {
        return values;
    }

    public void setValues(List<List<String>> values) {
        this.values = values;
    }
}
