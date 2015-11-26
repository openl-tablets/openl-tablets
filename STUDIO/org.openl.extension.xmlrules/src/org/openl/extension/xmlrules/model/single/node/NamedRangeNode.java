package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.ProjectData;

@XmlType(name = "named-range-node")
public class NamedRangeNode extends Node {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RangeNode getRangeNode() {
        return ProjectData.getCurrentInstance().getNamedRanges().get(name);
    }

    @Override
    public String toOpenLString() {
        return getRangeNode().toOpenLString();
    }
}
