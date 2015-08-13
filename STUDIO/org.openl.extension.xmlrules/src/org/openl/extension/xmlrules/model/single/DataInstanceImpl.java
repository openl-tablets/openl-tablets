package org.openl.extension.xmlrules.model.single;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.DataInstance;

@XmlRootElement(name="data-instance")
@XmlType(name = "data-instance")
public class DataInstanceImpl implements DataInstance {
    private String type;
    private String name;
    private List<String> fields;
    private List<Reference> references;
    private List<ValuesRow> values;

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

    @XmlElementWrapper(name="fields")
    @XmlElement(name = "string")
    @Override
    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    @XmlElementWrapper(name="references")
    @XmlElement(name = "reference")
    @Override
    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    @XmlElementWrapper(name="values", required = true)
    @XmlElement(name = "row")
    @Override
    public List<ValuesRow> getValues() {
        return values;
    }

    public void setValues(List<ValuesRow> values) {
        this.values = values;
    }
}
