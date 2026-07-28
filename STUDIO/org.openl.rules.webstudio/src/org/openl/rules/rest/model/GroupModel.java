package org.openl.rules.rest.model;

import lombok.Getter;

public class GroupModel {

    @Getter
    private String name;

    @Getter
    private GroupType type;

    public GroupModel setName(String name) {
        this.name = name;
        return this;
    }

    public GroupModel setType(GroupType type) {
        this.type = type;
        return this;
    }
}
