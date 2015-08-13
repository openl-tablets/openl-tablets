package org.openl.extension.xmlrules.model.single;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.Type;

@XmlRootElement(name="type")
@XmlType(name = "type")
public class TypeImpl implements Type {
    private String name;
    private List<FieldImpl> fields;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElementWrapper(name="fields", required = true)
    @XmlElement(name = "field")
    @Override
    public List<FieldImpl> getFields() {
        return fields;
    }

    public void setFields(List<FieldImpl> fields) {
        this.fields = fields;
    }

}
