package org.openl.rules.tbasic.runtime;


public class Result {

    private ReturnType type;
    private Object value;

    public Result(ReturnType returnType) {
        type = returnType;
    }

    public Result(ReturnType returnType, Object returnValue) {
        this(returnType);
        value = returnValue;
    }

    public Object getValue() {
        return value;
    }

    /**
     * @param returnType the returnType to set
     */
    public void setReturnType(ReturnType returnType) {
        type = returnType;
    }

    /**
     * @return the returnType
     */
    public ReturnType getReturnType() {
        return type;
    }

}
