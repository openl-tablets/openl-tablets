package org.openl.extension.xmlrules.model.single;

import java.util.List;

import org.openl.extension.xmlrules.model.Field;
import org.openl.extension.xmlrules.model.Type;

public class TypeImpl implements Type {
    private String name;
    private List<Field> fields;
    private XlsRegionImpl region;

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
    public XlsRegionImpl getRegion() {
        return region;
    }

    public void setRegion(XlsRegionImpl region) {
        this.region = region;
    }
}
