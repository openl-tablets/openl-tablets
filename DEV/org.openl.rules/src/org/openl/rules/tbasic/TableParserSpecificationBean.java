/**
 *
 */
package org.openl.rules.tbasic;

/**
 * @author User
 *
 */
public class TableParserSpecificationBean {
    public enum ValueNecessity {
        REQUIRED,
        OPTIONAL,
        PROHIBITED;
    }

    private String keyword;
    private String description;
    private boolean multiline;
    private ValueNecessity idents;
    private ValueNecessity condition;
    private ValueNecessity action;
    private ValueNecessity label;
    private ValueNecessity beforeAndAfter;
    private ValueNecessity topLevel;
    private boolean loopOperation;

    private String[] predecessorOperations;

    /**
     * @return the action
     */
    public ValueNecessity getAction() {
        return action;
    }

    /**
     * @return the beforeAndAfter
     */
    public ValueNecessity getBeforeAndAfter() {
        return beforeAndAfter;
    }

    /**
     * @return the condition
     */
    public ValueNecessity getCondition() {
        return condition;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the idents
     */
    public ValueNecessity getIdents() {
        return idents;
    }

    /**
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * @return the label
     */
    public ValueNecessity getLabel() {
        return label;
    }

    /**
     * @return the predecessorOperations
     */
    public String[] getPredecessorOperations() {
        return predecessorOperations;
    }

    /**
     * @return the topLevel
     */
    public ValueNecessity getTopLevel() {
        return topLevel;
    }

    /**
     * @return the loopOperation
     */
    public boolean isLoopOperation() {
        return loopOperation;
    }

    /**
     * @return the isMultiline
     */
    public boolean isMultiline() {
        return multiline;
    }

    /**
     * @param action the action to set
     */
    public void setAction(ValueNecessity action) {
        this.action = action;
    }

    /**
     * @param beforeAndAfter the beforeAndAfter to set
     */
    public void setBeforeAndAfter(ValueNecessity beforeAndAfter) {
        this.beforeAndAfter = beforeAndAfter;
    }

    /**
     * @param condition the condition to set
     */
    public void setCondition(ValueNecessity condition) {
        this.condition = condition;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param idents the idents to set
     */
    public void setIdents(ValueNecessity idents) {
        this.idents = idents;
    }

    /**
     * @param keyword the keyword to set
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(ValueNecessity label) {
        this.label = label;
    }

    /**
     * @param loopOperation the loopOperation to set
     */
    public void setLoopOperation(boolean loopOperation) {
        this.loopOperation = loopOperation;
    }

    /**
     * @param isMultiline the isMultiline to set
     */
    public void setMultiline(boolean isMultiline) {
        multiline = isMultiline;
    }

    /**
     * @param predecessorOperations the predecessorOperations to set
     */
    public void setPredecessorOperations(String[] predecessorOperations) {
        this.predecessorOperations = predecessorOperations;
    }

    /**
     * @param topLevel the topLevel to set
     */
    public void setTopLevel(ValueNecessity topLevel) {
        this.topLevel = topLevel;
    }
}
