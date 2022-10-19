package org.openl.rules.project.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import static org.openl.rules.project.xml.XmlRulesDeploySerializer.MODULE_NAME;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.NAME_TAG;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = MODULE_NAME)
public class WildcardPattern {
    @XmlAttribute(name = NAME_TAG)
    String value;

    public WildcardPattern(String value) {
        this.value = value;
    }

    // for JAXB serialization
    @SuppressWarnings("unused")
    private WildcardPattern() {}

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}