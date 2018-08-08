package org.openl.rules.webstudio.web.test.export;

public class B {
    private String id;
    private A[] aValues;
    private B[] childBValues;

    public B(String id, A... aValues) {
        this.id = id;
        this.aValues = aValues;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public A[] getaValues() {
        return aValues;
    }

    public void setaValues(A[] aValues) {
        this.aValues = aValues;
    }

    public B[] getChildBValues() {
        return childBValues;
    }

    public void setChildBValues(B... childBValues) {
        this.childBValues = childBValues;
    }
}
