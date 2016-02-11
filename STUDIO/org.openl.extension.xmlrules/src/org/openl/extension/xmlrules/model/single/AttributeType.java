package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "attribute-type")
@XmlEnum
public enum AttributeType {
    NUMBER,
    STRING,
    DATE
}
