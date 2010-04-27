package org.openl.rules.testmethod;

import org.openl.types.impl.DynamicObject;

public class TestStruct {

    private DynamicObject testObj;
    private Object res;
    private Throwable ex;

    public TestStruct(DynamicObject obj, Object res, Throwable ex) {
        this.ex = ex;
        this.res = res;
        this.testObj = obj;
    }

    public DynamicObject getTestObj() {
        return testObj;
    }

    public Object getRes() {
        return res;
    }

    public Throwable getEx() {
        return ex;
    }

}
