package org.openl.extension.xmlrules.model;

import javax.xml.bind.annotation.XmlElement;

public interface Expression {
    String getValue();

    Boolean getReference();

    Integer getWidth();

    Integer getHeight();
}
