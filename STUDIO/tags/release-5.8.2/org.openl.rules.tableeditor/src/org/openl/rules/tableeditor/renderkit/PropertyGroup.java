package org.openl.rules.tableeditor.renderkit;

import java.util.List;

public class PropertyGroup {

    private String group;

    private List<TableProperty> properties;

    public PropertyGroup() {
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<TableProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<TableProperty> properties) {
        this.properties = properties;
    }

}
