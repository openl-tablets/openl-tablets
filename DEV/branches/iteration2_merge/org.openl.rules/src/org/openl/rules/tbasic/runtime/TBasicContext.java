package org.openl.rules.tbasic.runtime;


public class TBasicContext {
    private Object openLTarget;
    private Object[] openLParams;
    private TBasicEnv tbasicEnvironment;
    
    /**
     * @param openLTarget
     * @param openLParams
     * @param openLEnvironment
     */
    public TBasicContext(Object openLTarget, Object[] openLParams, TBasicEnv environment) {
        this.openLTarget = openLTarget;
        this.openLParams = openLParams;
        this.tbasicEnvironment = environment;
    }

    

    /**
     * @return the openLTarget
     */
    public Object getOpenLTarget() {
        return openLTarget;
    }

    /**
     * @param openLTarget the openLTarget to set
     */
    public void setOpenLTarget(Object openLTarget) {
        this.openLTarget = openLTarget;
    }

    /**
     * @return the openLParams
     */
    public Object[] getOpenLParams() {
        return openLParams;
    }

    /**
     * @param openLParams the openLParams to set
     */
    public void setOpenLParams(Object[] openLParams) {
        this.openLParams = openLParams;
    }

    /**
     * @return the openLEnvironment
     */
    public TBasicEnv getOpenLEnvironment() {
        return tbasicEnvironment;
    }

    /**
     * @param openLEnvironment the openLEnvironment to set
     */
    public void setOpenLEnvironment(TBasicEnv openLEnvironment) {
        this.tbasicEnvironment = openLEnvironment;
    }



    public void assignValueToVariable(String variableName, Object value) {
        tbasicEnvironment.getTbasicTarget().setFieldValue(variableName, value);
    }
    

}
