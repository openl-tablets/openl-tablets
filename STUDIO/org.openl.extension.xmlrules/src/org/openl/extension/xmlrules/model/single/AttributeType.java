
package org.openl.extension.xmlrules.model.single;

import java.util.Date;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for attribute-type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="attribute-type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="NUMBER"/>
 *     &lt;enumeration value="STRING"/>
 *     &lt;enumeration value="DATE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "attribute-type")
@XmlEnum
public enum AttributeType {

    NUMBER(Double.class),
    STRING(String.class),
    DATE(Date.class);

    AttributeType(Class mappedType) {
        this.mappedType = mappedType;
    }

    public static AttributeType fromValue(String v) {
        return valueOf(v);
    }

    public Class getMappedType() {
        return mappedType;
    }

    private final Class mappedType;

}
