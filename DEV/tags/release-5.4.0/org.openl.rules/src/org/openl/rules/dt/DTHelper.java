package org.openl.rules.dt;

public class DTHelper {

    public static boolean isValidConditionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == 'C' && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidActionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == 'A' && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidRetHeader(String s) {
        return s.length() >= 3 && s.startsWith(IDecisionTableConstants.RETURN) && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    public static boolean isValidRuleHeader(String s) {
        return s.equals(IDecisionTableConstants.RULE);
    }

    public static boolean isValidCommentHeader(String s) {
        return s.startsWith("//");
    }

    public static boolean isActionHeader(String s) {
        return isValidActionHeader(s) || isValidRetHeader(s);
    }

    public static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || DTLookupConvertor.isValidHConditionHeader(s);
    }
    
}
