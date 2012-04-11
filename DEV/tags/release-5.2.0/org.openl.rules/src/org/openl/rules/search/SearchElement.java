/**
 * Created May 4, 2007
 */
package org.openl.rules.search;

/**
 * @author snshor
 *
 */
public class SearchElement implements ISearchConstants {

    GroupOperator operator = new GroupOperator.AND();
    boolean notFlag = false;
    protected String type;
    String opType1;
    String value1 = ANY;
    String opType2;
    String value2 = ANY;

    public SearchElement(String type) {
        this.type = type;
    }

    public SearchElement copy() {
        SearchElement cpy = new SearchElement(type);

        cpy.notFlag = notFlag;
        cpy.operator = operator;
        cpy.opType1 = opType1;
        cpy.value1 = value1;
        cpy.opType2 = opType2;
        cpy.value2 = value2;

        return cpy;
    }

    public GroupOperator getOperator() {
        return operator;
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

    public String getValue1() {
        return value1;
    }

    public String getValue2() {
        return value2;
    }

    public boolean isAny(String value) {
        return value == null || value.trim().length() == 0 || ANY.equals(value);
    }

    public boolean isNotFlag() {
        return notFlag;
    }

    public void setNotFlag(boolean notFlag) {
        this.notFlag = notFlag;
    }

    public void setOperator(GroupOperator operator) {
        this.operator = operator;
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

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }
}
