/**
 * 
 */
package org.openl.rules.tbasic.runtime;

/**
 * @author User
 * 
 */
public class ConditionalGotoOperation extends GotoOperation {

    protected boolean expectedCondition;

    public ConditionalGotoOperation(String label, boolean expectedCondition) {
        super(label);
        this.expectedCondition = expectedCondition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules
     * .tbasic.runtime.TBasicContext, java.lang.Object[])
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        if (param == null || !(param instanceof Boolean)) {
            // FIXME Add source reference and understandable hint
            throw new IllegalArgumentException("Previous operation should return boolean value");
        }

        boolean condition = (Boolean) param;
        Result result;

        if (condition == expectedCondition) {
            result = executeUnconditionalGoto(environment);
        } else {
            result = executeSkipGoto();
        }

        return result;
    }

    /**
     * @return
     */
    private Result executeSkipGoto() {
        Result result;
        result = new Result(ReturnType.Next);
        return result;
    }

    /**
     * @param context
     * @return
     */
    private Result executeUnconditionalGoto(TBasicContextHolderEnv environment) {
        Result result;
        result = super.execute(environment, null);
        return result;
    }
}
