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
    private String idents;
    private String condition;
    private String action;
    private String label;
    private String beforeAndAfter;
    private String topLevel;
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
     * @return the isMultiline
     */
    public boolean isMultiline() {
        return multiline;
    }

    /**
     * @param isMultiline
     *            the isMultiline to set
     */
    public void setMultiline(boolean isMultiline) {
        this.multiline = isMultiline;
    }

    /**
     * @return the idents
     */
    public String getIdents() {
        return idents;
    }

    /**
     * @param idents the idents to set
     */
    public void setIdents(String idents) {
        this.idents = idents;
    }

    /**
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * @param condition the condition to set
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the beforeAndAfter
     */
    public String getBeforeAndAfter() {
        return beforeAndAfter;
    }

    /**
     * @param beforeAndAfter the beforeAndAfter to set
     */
    public void setBeforeAndAfter(String beforeAndAfter) {
        this.beforeAndAfter = beforeAndAfter;
    }

    /**
     * @return the topLevel
     */
    public String getTopLevel() {
        return topLevel;
    }

    /**
     * @param topLevel the topLevel to set
     */
    public void setTopLevel(String topLevel) {
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

    private enum ValueNecessity {
        REQUIRED,
        OPTIONAL,
        PROHIBITED;
    }

    public boolean isRequired(String value) {
        return value != null && value.equalsIgnoreCase(ValueNecessity.REQUIRED.name());
    }

    public boolean isOptional(String value) {
        return value != null && value.equalsIgnoreCase(ValueNecessity.OPTIONAL.name());
    }

    public boolean isProhibited(String value) {
        return value != null && value.equalsIgnoreCase(ValueNecessity.PROHIBITED.name());
    }

}
