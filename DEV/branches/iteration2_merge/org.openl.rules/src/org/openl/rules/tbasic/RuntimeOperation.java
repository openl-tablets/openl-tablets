package org.openl.rules.tbasic;

public abstract class RuntimeOperation {

    public abstract Result execute(TBasicContext context, Object... objects);
}
