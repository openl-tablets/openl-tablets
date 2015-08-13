package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "reference")
public class Reference {
    private String field;
    private String dataInstance;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @XmlElement(name = "data-instance")
    public String getDataInstance() {
        return dataInstance;
    }

    public void setDataInstance(String dataInstance) {
        this.dataInstance = dataInstance;
    }
}
