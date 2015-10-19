package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum Comparison {
    @XmlEnumValue("None") NONE(null),
    @XmlEnumValue("LessThan") LESS("<"),
    @XmlEnumValue("LessThanOrEqual") LESS_EQUAL("<="),
    @XmlEnumValue("EqualTo") EQUAL("=="),
    @XmlEnumValue("GreaterThan") GREATER(">"),
    @XmlEnumValue("GreaterThanOrEqual") GREATER_EQUAL(">="),
    ;

    private final String value;

    Comparison(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
