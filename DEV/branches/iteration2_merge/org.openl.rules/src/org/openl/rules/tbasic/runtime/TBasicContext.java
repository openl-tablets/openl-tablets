package org.openl.rules.tbasic.runtime;

import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;

public class TBasicContext {
    private DelegatedDynamicObject thisTarget;
    private Object openLTarget;
    private Object[] openLParams;
    private IRuntimeEnv openLEnvironment;
    
    /**
     * @param openLTarget
     * @param openLParams
     * @param openLEnvironment
     */
    public TBasicContext(DelegatedDynamicObject thisTarget, Object openLTarget, Object[] openLParams, IRuntimeEnv openLEnvironment) {
        super();
        this.setThisTarget(thisTarget);
        this.openLTarget = openLTarget;
        this.openLParams = openLParams;
        this.openLEnvironment = openLEnvironment;
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
    public IRuntimeEnv getOpenLEnvironment() {
        return openLEnvironment;
    }

    /**
     * @param openLEnvironment the openLEnvironment to set
     */
    public void setOpenLEnvironment(IRuntimeEnv openLEnvironment) {
        this.openLEnvironment = openLEnvironment;
    }

    /**
     * @param thisTarget the thisTarget to set
     */
    public void setThisTarget(DelegatedDynamicObject thisTarget) {
        this.thisTarget = thisTarget;
    }

    /**
     * @return the thisTarget
     */
    public DelegatedDynamicObject getThisTarget() {
        return thisTarget;
    }



    public void assignValueToVariable(String variableName, Object value) {
        thisTarget.setFieldValue(variableName, value);
    }
    

}
