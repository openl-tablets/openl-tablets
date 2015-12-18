package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.Expression;

@XmlType(name = "expression")
public class ExpressionImpl implements Expression {
    private String value;
    private Boolean reference = Boolean.FALSE;
    private Integer width = 1;
    private Integer height = 1;

    @XmlElement(required = true)
    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @XmlElement(defaultValue = "false")
    @Override
    public Boolean getReference() {
        return reference;
    }

    public void setReference(Boolean reference) {
        this.reference = reference;
    }

    @XmlElement(defaultValue = "1")
    @Override
    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    @XmlElement(defaultValue = "1")
    @Override
    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
