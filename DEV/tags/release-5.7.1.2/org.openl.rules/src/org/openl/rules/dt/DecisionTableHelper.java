package org.openl.rules.dt;

import org.openl.rules.table.ILogicalTable;

public class DecisionTableHelper {

    public static boolean looksLikeTransposed(ILogicalTable table) {

        if (table.getLogicalWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return true;
        }

        if (table.getLogicalHeight() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return false;
        }

        int cnt1 = countConditionsAndActions(table);
        int cnt2 = countConditionsAndActions(table.transpose());

        if (cnt1 != cnt2) {
            return cnt1 > cnt2;
        }

        return table.getLogicalWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;
    }
    
    public static boolean isValidConditionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.CONDITION.getHeaderKey().charAt(0) 
            && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidActionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.ACTION.getHeaderKey().charAt(0) 
            && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidRetHeader(String s) {
        return s.length() >= 3 && s.startsWith(DecisionTableColumnHeaders.RETURN.getHeaderKey()) && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    public static boolean isValidRuleHeader(String s) {
        return s.equals(DecisionTableColumnHeaders.RULE.getHeaderKey());
    }

    public static boolean isValidCommentHeader(String s) {
        return s.startsWith("//");
    }

    public static boolean isActionHeader(String s) {
        return isValidActionHeader(s) || isValidRetHeader(s);
    }

    public static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || isValidHConditionHeader(s);
    }

    public static int countConditionsAndActions(ILogicalTable table) {

        int width = table.getLogicalWidth();
        int count = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();
                count += isValidConditionHeader(value) || isActionHeader(value) ? 1 : 0;
            }
        }

        return count;
    }

    public static boolean hasHConditions(ILogicalTable table) {

        int width = table.getLogicalWidth();

        for (int i = 0; i < width; i++) {

            String value = table.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();

                if (isValidHConditionHeader(value)) {
                    return true;
                }
            }
        }

        return false;
    }
    
    public static boolean isValidHConditionHeader(String headerStr) {
        return headerStr.startsWith(DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character.isDigit(headerStr.charAt(2));
    }

}
