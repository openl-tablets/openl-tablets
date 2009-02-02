package org.openl.rules.tbasic;

import org.openl.vm.IRuntimeEnv;

public class TBasicContext {
    /**
     * @param openLTarget
     * @param openLParams
     * @param openLEnvironment
     */
    public TBasicContext(Object openLTarget, Object[] openLParams, IRuntimeEnv openLEnvironment) {
        super();
        this.openLTarget = openLTarget;
        OpenLParams = openLParams;
        this.openLEnvironment = openLEnvironment;
    }

    private Object openLTarget;
    private Object[] OpenLParams;
    private IRuntimeEnv openLEnvironment;

    /**
     * @return the openLTarget
     */
    public Object getOpenLTarget() {
        return openLTarget;
    }

    /**
     * @param openLTarget
     *            the openLTarget to set
     */
    public void setOpenLTarget(Object openLTarget) {
        this.openLTarget = openLTarget;
    }

    /**
     * @return the openLParams
     */
    public Object[] getOpenLParams() {
        return OpenLParams;
    }

    /**
     * @param openLParams
     *            the openLParams to set
     */
    public void setOpenLParams(Object[] openLParams) {
        OpenLParams = openLParams;
    }

    /**
     * @return the openLEnvironment
     */
    public IRuntimeEnv getOpenLEnvironment() {
        return openLEnvironment;
    }

    /**
     * @param openLEnvironment
     *            the openLEnvironment to set
     */
    public void setOpenLEnvironment(IRuntimeEnv openLEnvironment) {
        this.openLEnvironment = openLEnvironment;
    }

}
