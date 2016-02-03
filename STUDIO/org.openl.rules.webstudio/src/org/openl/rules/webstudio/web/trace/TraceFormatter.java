package org.openl.rules.webstudio.web.trace;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.binding.MethodUtil;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.trace.SpreadsheetTracerLeaf;
import org.openl.rules.cmatch.algorithm.MatchTraceObject;
import org.openl.rules.cmatch.algorithm.ResultTraceObject;
import org.openl.rules.cmatch.algorithm.WScoreTraceObject;
import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.dtx.trace.DTConditionTraceObject;
import org.openl.rules.dtx.trace.DTIndexedTraceObject;
import org.openl.rules.dtx.trace.DTRuleTracerLeaf;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.tbasic.compile.AlgorithmOperationSource;
import org.openl.rules.tbasic.runtime.debug.TBasicMethodTraceObject;
import org.openl.rules.tbasic.runtime.debug.TBasicOperationTraceObject;
import org.openl.rules.types.impl.OverloadedMethodChoiceTraceObject;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.trace.ITracerObject;

public class TraceFormatter {
    static String getDisplayName(ITracerObject obj, int mode) {
        if (obj instanceof WScoreTraceObject) {
            return "Score: " + obj.getResult();
        } else if (obj instanceof ResultTraceObject) {
            return "Result: " + obj.getResult();
        } else if (obj instanceof MatchTraceObject) {
            return getDisplayName((MatchTraceObject) obj);
        } else if (obj instanceof TBasicOperationTraceObject) {
            return getDisplayName((TBasicOperationTraceObject) obj);
        } else if (obj instanceof TBasicMethodTraceObject) {
            return getDisplayName((TBasicMethodTraceObject) obj, mode);
        } else if (obj instanceof DTIndexedTraceObject) {
            return getDisplayName((DTIndexedTraceObject) obj);
        } else if (obj instanceof DTConditionTraceObject) {
            return getDisplayName((DTConditionTraceObject) obj);
        } else if (obj instanceof SpreadsheetTracerLeaf) {
            return getDisplayName((SpreadsheetTracerLeaf) obj);
        } else if (obj instanceof OverloadedMethodChoiceTraceObject) {
            return "Overloaded method choice for method " + MethodUtil
                .printSignature(((OverloadedMethodChoiceTraceObject) obj).getMethodCandidates().get(0), 0);
        } else if (obj instanceof DTRuleTracerLeaf) {
            return getDisplayName((DTRuleTracerLeaf) obj);
        } else if (obj instanceof ATableTracerNode) {
            return getDisplayName((ATableTracerNode) obj);
        }
        return "NULL - " + obj.getClass();
    }

    private static String getDisplayName(DTRuleTracerLeaf dtr) {
        return String.format("Returned rule: %s",
            ((IDecisionTable) dtr.getParentTraceObject().getTraceObject()).getRuleName(dtr.getRuleIndex()));
    }

    private static String getDisplayName(MatchTraceObject mto) {
        String operation = mto.getOperation();
        String checkValue = String.valueOf(mto.getCheckValue());
        Object result = mto.getResult();
        String txt = "Match: " + operation + " " + checkValue;
        if (result != null) {
            txt += " = " + FormattersManager.format(result);
        }
        return txt;
    }

    private static String getDisplayName(TBasicOperationTraceObject tbo) {
        AlgorithmOperationSource sourceCode = tbo.getSourceCode();
        String nameForDebug = tbo.getNameForDebug();
        Object result = tbo.getResult();
        HashMap<String, Object> fieldValues = tbo.getFieldValues();

        String operationName = sourceCode.getOperationName();
        String resultValue = "";
        if (result != null) {
            resultValue = "(" + result.toString() + ")";
        }
        int operationRow = sourceCode.getRowNumber();

        String fieldFormatedValues = getFieldValuesAsString(fieldValues);

        String displayFieldFormatedValues = "";
        if (!fieldFormatedValues.equals("")) {
            displayFieldFormatedValues = String.format("[Local vars: %s]", fieldFormatedValues);
        }

        return String.format("Step: row %d: %s %s %s %s",
            operationRow,
            operationName,
            (nameForDebug != null ? nameForDebug : ""),
            resultValue,
            displayFieldFormatedValues);
    }

    private static String getFieldValuesAsString(HashMap<String, Object> fieldValues) {
        StringBuilder fields = new StringBuilder();

        for (String fieldName : fieldValues.keySet()) {
            Object value = fieldValues.get(fieldName);
            String formattedValue = FormattersManager.format(value);
            fields.append(fieldName).append(" = ").append(formattedValue).append(", ");
        }

        // remove last ", "
        if (fields.length() > 2) {
            fields.delete(fields.length() - 2, fields.length());
        }

        return fields.toString();
    }

    private static String getDisplayName(TBasicMethodTraceObject tbm, int mode) {

        AlgorithmSubroutineMethod method = tbm.getMethod();
        Object result = tbm.getResult();
        String returnValue = "";
        IOpenClass returnType = method.getType();
        if (!JavaOpenClass.isVoid(returnType)) {
            returnValue = String.format("%s = %s",
                returnType.getDisplayName(mode),
                result != null ? result.toString() : "null");
        }

        String displayName = method.getHeader().getDisplayName(mode);

        return String.format("Algorithm Method %s %s", returnValue, displayName);
    }

    private static String getDisplayName(DTIndexedTraceObject dti) {
        int[] rules = dti.getLinkedRule();
        IDecisionTable decisionTable = ((IDecisionTable) dti.getTraceObject());

        String[] ruleNames = new String[rules.length];
        for (int i = 0; i < ruleNames.length; i++) {
            ruleNames[i] = decisionTable.getRuleName(rules[i]);
        }

        return String.format("Indexed condition: %s, Rules: %s", dti.getConditionName(), Arrays.toString(ruleNames));
    }

    private static String getDisplayName(DTConditionTraceObject dtc) {
        return String.format("Rule: %s, Condition: %s",
            ((IDecisionTable) dtc.getTraceObject()).getRuleName(dtc.getRuleIndex()),
            dtc.getConditionName());
    }

    private static String getDisplayName(SpreadsheetTracerLeaf stl) {
        StringBuilder buf = new StringBuilder(64);
        Spreadsheet spreadsheet = (Spreadsheet) stl.getTraceObject();
        buf.append(SpreadsheetStructureBuilder.DOLLAR_SIGN);
        SpreadsheetCell spreadsheetCell = stl.getSpreadsheetCell();
        buf.append(spreadsheet.getColumnNames()[spreadsheetCell.getColumnIndex()]);
        buf.append(SpreadsheetStructureBuilder.DOLLAR_SIGN);
        buf.append(spreadsheet.getRowNames()[spreadsheetCell.getRowIndex()]);

        if (!JavaOpenClass.isVoid(spreadsheetCell.getType())) {
            /** write result for all cells, excluding void type */
            Object result = stl.getResult();
            String txt;
            if (result != null && result.getClass().isArray()) {
                txt = ArrayUtils.toString(result);
            } else {
                txt = FormattersManager.format(result);
            }

            buf.append(" = ").append(txt);
        }
        return buf.toString();
    }

    private static String getDisplayName(ATableTracerNode attn) {
        StringBuilder buf = new StringBuilder(64);
        buf.append(attn.getPrefix()).append(' ');

        ExecutableRulesMethod method = attn.getTraceObject();
        MethodUtil.printMethod(method, buf);

        if (attn.hasError()) {
            // append error of any
            buf.append(" = ERROR");
        } else {
            IOpenClass type = method.getType();
            if (!JavaOpenClass.isVoid(type)) {
                // append formatted result
                buf.append(" = ").append(FormattersManager.format(attn.getResult()));
            }
        }

        return buf.toString();
    }

}
