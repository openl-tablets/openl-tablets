package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum Comparison {
    @XmlEnumValue("<") LESS("<"),
    @XmlEnumValue("<=") LESS_EQUAL("<="),
    @XmlEnumValue("==") EQUAL("=="),
    @XmlEnumValue(">") GREATER(">"),
    @XmlEnumValue(">=") GREATER_EQUAL(">="),
    ;

    private final String value;

    Comparison(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
