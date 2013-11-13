package org.openl.rules.webstudio.web.repository;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class DeploymentProjectItem extends AbstractItem {
    private static final long serialVersionUID = -8514729162777803447L;

    /** Disabled item cannot be selected */
    private boolean disabled;

    public boolean isDisabled() {
        return disabled;
    }

    /**
     * If item is disabled then return <code>false</code> always.
     */
    @Override
    public boolean isSelected() {
        if (disabled) {
            return false;
        }

        return super.isSelected();
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

}
