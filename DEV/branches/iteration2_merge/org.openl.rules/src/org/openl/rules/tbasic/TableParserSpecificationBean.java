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
    private boolean multiLine;
    private boolean canHaveIdents;
    private boolean mustHaveCondition;
    private boolean mustHaveAction;
    private boolean obligatoryLabel;
    private boolean canHaveBeforeAndAfter;
    private boolean canBeOnlyTopLevel;
    private boolean loopOperation;
    private String[] predecessorOperations;

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
     * @return the mustHaveCondition
     */
    public boolean isMustHaveCondition() {
        return mustHaveCondition;
    }

    /**
     * @param mustHaveCondition the mustHaveCondition to set
     */
    public void setMustHaveCondition(boolean mustHaveCondition) {
        this.mustHaveCondition = mustHaveCondition;
    }

    /**
     * @return the mustHaveAction
     */
    public boolean isMustHaveAction() {
        return mustHaveAction;
    }

    /**
     * @param mustHaveAction the mustHaveAction to set
     */
    public void setMustHaveAction(boolean mustHaveAction) {
        this.mustHaveAction = mustHaveAction;
    }

    /**
     * @return the obligatoryLabel
     */
    public boolean isObligatoryLabel() {
        return obligatoryLabel;
    }

    /**
     * @param obligatoryLabel the obligatoryLabel to set
     */
    public void setObligatoryLabel(boolean obligatoryLabel) {
        this.obligatoryLabel = obligatoryLabel;
    }

    /**
     * @return the canHaveBeforeAndAfter
     */
    public boolean isCanHaveBeforeAndAfter() {
        return canHaveBeforeAndAfter;
    }

    /**
     * @param canHaveBeforeAndAfter the canHaveBeforeAndAfter to set
     */
    public void setCanHaveBeforeAndAfter(boolean canHaveBeforeAndAfter) {
        this.canHaveBeforeAndAfter = canHaveBeforeAndAfter;
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
    
    /**
     * @return the loopOperation
     */
    public boolean isLoopOperation() {
        return loopOperation;
    }

    /**
     * @param loopOperation the loopOperation to set
     */
    public void setLoopOperation(boolean loopOperation) {
        this.loopOperation = loopOperation;
    }

    /**
     * @return the predecessorOperations
     */
    public String[] getPredecessorOperations() {
        return predecessorOperations;
    }

    /**
     * @param predecessorOperations the predecessorOperations to set
     */
    public void setPredecessorOperations(String[] predecessorOperations) {
        this.predecessorOperations = predecessorOperations;
    }

}
