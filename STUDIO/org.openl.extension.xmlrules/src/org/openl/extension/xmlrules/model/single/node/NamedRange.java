package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "named-range")
public class NamedRange {
    private String name;
    private RangeNode range;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RangeNode getRange() {
        return range;
    }

    public void setRange(RangeNode range) {
        this.range = range;
    }
}
