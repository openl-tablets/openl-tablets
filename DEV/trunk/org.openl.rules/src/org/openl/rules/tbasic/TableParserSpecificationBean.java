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
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * @param keyword the keyword to set
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
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the isMultiline
     */
    public boolean isMultiline() {
        return multiline;
    }

    /**
     * @param isMultiline the isMultiline to set
     */
    public void setMultiline(boolean isMultiline) {
        this.multiline = isMultiline;
    }

    /**
     * @return the idents
     */
    public ValueNecessity getIdents() {
        return idents;
    }

    /**
     * @param idents the idents to set
     */
    public void setIdents(ValueNecessity idents) {
        this.idents = idents;
    }

    /**
     * @return the condition
     */
    public ValueNecessity getCondition() {
        return condition;
    }

    /**
     * @param condition the condition to set
     */
    public void setCondition(ValueNecessity condition) {
        this.condition = condition;
    }

    /**
     * @return the action
     */
    public ValueNecessity getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(ValueNecessity action) {
        this.action = action;
    }

    /**
     * @return the label
     */
    public ValueNecessity getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(ValueNecessity label) {
        this.label = label;
    }

    /**
     * @return the beforeAndAfter
     */
    public ValueNecessity getBeforeAndAfter() {
        return beforeAndAfter;
    }

    /**
     * @param beforeAndAfter the beforeAndAfter to set
     */
    public void setBeforeAndAfter(ValueNecessity beforeAndAfter) {
        this.beforeAndAfter = beforeAndAfter;
    }

    /**
     * @return the topLevel
     */
    public ValueNecessity getTopLevel() {
        return topLevel;
    }

    /**
     * @param topLevel the topLevel to set
     */
    public void setTopLevel(ValueNecessity topLevel) {
        this.topLevel = topLevel;
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

    public enum ValueNecessity {
        REQUIRED,
        OPTIONAL,
        PROHIBITED;
    }
}
