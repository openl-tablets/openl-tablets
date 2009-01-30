/**
 * 
 */
package org.openl.rules.tbasic;

/**
 * @author User
 * 
 */
public class TableParserSpecificationBean {
    private String keyword;
    private String description;
    private boolean canHaveIdents;
    private boolean multiLine;
    private boolean condition;
    private boolean action;
    private boolean label;
    private boolean beforeAndAfter;
    private boolean canBeOnlyTopLevel;

    /**
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * @param keyword
     *            the keyword to set
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the canHaveIdents
     */
    public boolean isCanHaveIdents() {
        return canHaveIdents;
    }

    /**
     * @param canHaveIdents
     *            the canHaveIdents to set
     */
    public void setCanHaveIdents(boolean canHaveIdents) {
        this.canHaveIdents = canHaveIdents;
    }

    /**
     * @return the isMultiLine
     */
    public boolean isMultiLine() {
        return multiLine;
    }

    /**
     * @param isMultiLine
     *            the isMultiLine to set
     */
    public void setMultiLine(boolean isMultiLine) {
        this.multiLine = isMultiLine;
    }

    /**
     * @return the condition
     */
    public boolean isCondition() {
        return condition;
    }

    /**
     * @param condition
     *            the condition to set
     */
    public void setCondition(boolean condition) {
        this.condition = condition;
    }

    /**
     * @return the action
     */
    public boolean isAction() {
        return action;
    }

    /**
     * @param action
     *            the action to set
     */
    public void setAction(boolean action) {
        this.action = action;
    }

    /**
     * @return the label
     */
    public boolean isLabel() {
        return label;
    }

    /**
     * @param label
     *            the label to set
     */
    public void setLabel(boolean label) {
        this.label = label;
    }

    /**
     * @return the beforeAndAfter
     */
    public boolean isBeforeAndAfter() {
        return beforeAndAfter;
    }

    /**
     * @param beforeAndAfter
     *            the beforeAndAfter to set
     */
    public void setBeforeAndAfter(boolean beforeAndAfter) {
        this.beforeAndAfter = beforeAndAfter;
    }

    /**
     * @return the canBeOnlyTopLevel
     */
    public boolean isCanBeOnlyTopLevel() {
        return canBeOnlyTopLevel;
    }

    /**
     * @param canBeOnlyTopLevel
     *            the canBeOnlyTopLevel to set
     */
    public void setCanBeOnlyTopLevel(boolean canBeOnlyTopLevel) {
        this.canBeOnlyTopLevel = canBeOnlyTopLevel;
    }

}
