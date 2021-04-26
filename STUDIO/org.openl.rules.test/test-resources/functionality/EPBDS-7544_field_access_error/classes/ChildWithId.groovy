package org.openl.rules.beans;

class ChildWithId extends AccessBean {

    public final String id = "childId";

    String getId() {
        return "getId()";
    }

}