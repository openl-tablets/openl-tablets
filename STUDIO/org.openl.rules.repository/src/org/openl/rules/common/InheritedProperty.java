package org.openl.rules.common;

import java.io.Serializable;

public class InheritedProperty implements Serializable {

    private static final long serialVersionUID = 1L;
    private Object value;
    private ArtefactType typeOfNode;
    private String nameOfNode;

    public InheritedProperty(Object value, ArtefactType typeOfNode, String nameOfNode) {
        this.value = value;
        this.typeOfNode = typeOfNode;
        this.nameOfNode = nameOfNode;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ArtefactType getTypeOfNode() {
        return typeOfNode;
    }

    public void setTypeOfNode(ArtefactType typeOfNode) {
        this.typeOfNode = typeOfNode;
    }

    public String getNameOfNode() {
        return nameOfNode;
    }

    public void setNameOfNode(String nameOfNode) {
        this.nameOfNode = nameOfNode;
    }

}
