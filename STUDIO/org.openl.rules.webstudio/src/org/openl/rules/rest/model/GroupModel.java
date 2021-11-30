package org.openl.rules.rest.model;

public class GroupModel {

    private String name;

    private GroupType type;

    public String getName() {
        return name;
    }

    public GroupModel setName(String name) {
        this.name = name;
        return this;
    }

    public GroupType getType() {
        return type;
    }

    public GroupModel setType(GroupType type) {
        this.type = type;
        return this;
    }
}
