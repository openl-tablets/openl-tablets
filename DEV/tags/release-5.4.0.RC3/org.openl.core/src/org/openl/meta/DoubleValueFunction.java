package org.openl.meta;

import java.util.Iterator;

import org.openl.util.OpenIterator;

public class DoubleValueFunction extends DoubleValue {
    /**
     *
     */
    private static final long serialVersionUID = -4252776600829254624L;
    String functionName;
    DoubleValue result;
    DoubleValue[] params = {};

    public DoubleValueFunction(double value, String functionName, DoubleValue[] params) {
        super(value);
        this.functionName = functionName;
        this.params = params;
        result = new DoubleValue(value);
    }

    public DoubleValueFunction(DoubleValue result, String functionName, DoubleValue[] params) {
        super(result.getValue());
        this.result = result;
        this.functionName = functionName;
        this.params = params;
    }

    public void addParam(DoubleValue param) {
        DoubleValue[] newParams = new DoubleValue[params.length + 1];
        for (int i = 0; i < params.length; i++) {
            newParams[i] = params[i];
        }
        newParams[params.length] = param;
        params = newParams;
    }

    @Override
    public Iterator<DoubleValue> getChildren() {
        return OpenIterator.fromArray(params);
    }

    @Override
    public String getDisplayName(int mode) {
        switch (mode) {
            case SHORT:
                return super.getDisplayName(mode);
            default:
                String f = functionName + '(';
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) {
                        f += ", ";
                    }
                    f += params[i].getDisplayName(mode - 1);
                }
                return super.getDisplayName(mode) + " = " + f + ')';
        }
    }

    public String getFunctionName() {
        return functionName;
    }

    public DoubleValue[] getParams() {
        return params;
    }

    public DoubleValue getResult() {
        return result;
    }

    @Override
    public String getType() {
        return "function";
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public String printContent(int mode, boolean fromMultiplicativeExpr, boolean inBrackets) {
        if ((mode & EXPAND_FUNCTION) == 0) {
            if (result == null) {
                return super.printContent(mode, false, inBrackets);
            }
            return result.printContent(mode, fromMultiplicativeExpr, inBrackets);
        }
        StringBuffer buf = new StringBuffer();

        if ((mode & PRINT_VALUE_IN_EXPANDED) != 0) {
            if (!inBrackets) {
                buf.append('(');
            }
            buf.append(printValue() + "=");
        }

        buf.append(functionName);
        buf.append('(');
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append(params[i].printExplanationLocal(mode, false));
        }
        buf.append(')');
        if (!inBrackets) {
            if ((mode & PRINT_VALUE_IN_EXPANDED) != 0) {
                buf.append(')');
            }
        }

        return buf.toString();
    }

    @Override
    public String printValue() {
        return result.printValue(format);
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public void setParams(DoubleValue[] params) {
        this.params = params;
    }

    public void setResult(DoubleValue result) {
        this.result = result;
        value = result.doubleValue();
    }

}
