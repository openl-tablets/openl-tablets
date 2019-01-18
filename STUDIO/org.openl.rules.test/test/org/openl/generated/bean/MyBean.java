package org.openl.generated.bean;

public class MyBean implements IMyBean {

    private String field1;
    private Integer field2;

    @Override
    public String getField1() {
        return field1;
    }

    @Override
    public void setField1(String field1) {
        this.field1 = field1;
    }

    @Override
    public Integer getField2() {
        return field2;
    }

    @Override
    public void setField2(Integer field2) {
        this.field2 = field2;
    }

}
