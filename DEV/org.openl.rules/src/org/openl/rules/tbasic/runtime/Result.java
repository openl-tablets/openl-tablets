package org.openl.rules.tbasic.runtime;

/**
 * The <code>Result</code> class stores result of execution some operation and command for VM which operation must be
 * next.
 *
 */
public class Result {

    private ReturnType type;
    private Object value;

    /**
     * Create an instance of <code>Result</code> for operation which didn't return any value. Initialized with order to
     * compiler what to do after current operation.
     *
     * @param returnType Order to compiler
     */
    public Result(ReturnType returnType) {
        type = returnType;
    }

    /**
     * Create an instance of <code>Result</code> for operation which returned value. Initialized with order to compiler
     * what to do after current operation.
     *
     * @param returnType Order to compiler
     * @param returnValue Result of execution of operation.
     */
    public Result(ReturnType returnType, Object returnValue) {
        this(returnType);
        value = returnValue;
    }

    /**
     * @return the returnType
     */
    public ReturnType getReturnType() {
        return type;
    }

    /**
     *
     * @return Result of execution of operation.
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param returnType the returnType to set
     */
    public void setReturnType(ReturnType returnType) {
        type = returnType;
    }

}
