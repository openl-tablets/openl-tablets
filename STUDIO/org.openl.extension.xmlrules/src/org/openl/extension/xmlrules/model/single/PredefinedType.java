package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlEnum
@XmlType
public enum PredefinedType {
    String,
    Double,
    Integer,
    Boolean,
    Date
}
