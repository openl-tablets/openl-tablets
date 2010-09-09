/**
 * Created May 4, 2007
 */
package org.openl.rules.search;

/**
 * @author snshor
 *
 */
public class SearchConditionElement implements ISearchConstants {

    private GroupOperator groupOperator = new GroupOperator.AND();
    private boolean notFlag = false;
    protected String type;
    private String opType1;
    private String elementValueName;
    private String opType2;
    private String elementValue;

    public SearchConditionElement(String type) {
        this.type = type;
    }

    public SearchConditionElement copy() {
        SearchConditionElement cpy = new SearchConditionElement(type);

        cpy.notFlag = notFlag;
        cpy.groupOperator = groupOperator;
        cpy.opType1 = opType1;
        cpy.elementValueName = elementValueName;
        cpy.opType2 = opType2;
        cpy.elementValue = elementValue;

        return cpy;
    }

    public GroupOperator getGroupOperator() {
        return groupOperator;
    }

    public String getOpType1() {
        return opType1;
    }

    public String getOpType2() {
        return opType2;
    }

    public String getType() {
        return type;
    }

    public String getElementValueName() {
        return elementValueName;
    }

    public String getElementValue() {
        return elementValue;
    }

    public boolean isAny(String value) {
        return value == null || value.trim().length() == 0;
    }

    public boolean isNotFlag() {
        return notFlag;
    }

    public void setNotFlag(boolean notFlag) {
        this.notFlag = notFlag;
    }

    public void setGroupOperator(GroupOperator operator) {
        this.groupOperator = operator;
    }

    public void setOpType1(String opType1) {
        this.opType1 = opType1;
    }

    public void setOpType2(String opType) {
        opType2 = opType;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setElementValueName(String elementValueName) {
        this.elementValueName = elementValueName;
    }

    public void setElementValue(String elementValue) {
        this.elementValue = elementValue;
    }
}
