package org.openl.rules.spring.openapi.app100.model;

public class Cat extends Animal {

    private boolean indoor;

    public boolean isIndoor() {
        return indoor;
    }

    public void setIndoor(boolean indoor) {
        this.indoor = indoor;
    }
}
