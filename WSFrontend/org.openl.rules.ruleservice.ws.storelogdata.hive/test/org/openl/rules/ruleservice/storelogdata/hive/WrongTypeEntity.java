package org.openl.rules.ruleservice.storelogdata.hive;

import org.openl.rules.ruleservice.storelogdata.hive.annotation.Entity;

@Entity("wrong_type")
public class WrongTypeEntity {
    private Object someObject;

    public WrongTypeEntity() {
    }

    public Object getSomeObject() {
        return someObject;
    }

    public void setSomeObject(Object someObject) {
        this.someObject = someObject;
    }
}