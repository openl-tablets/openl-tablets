package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class TableSyntaxNodeHelper {

    private TableSyntaxNodeHelper() {
    }

    private static final Pattern REGEX = Pattern.compile("[ ,()\t\n\r\\[\\]]");

    public static String getSignature(TableSyntaxNode rableSyntaxNode) {
        return rableSyntaxNode.getHeader().getHeaderToken().getModule().getCode();
    }

    public static String getTableReturnType(TableSyntaxNode tableSyntaxNode) {
        String[] tokens = REGEX.split(getSignature(tableSyntaxNode));
        if (tokens.length > 3) {
            List<String> notEmptyTokens = new ArrayList<>();
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    notEmptyTokens.add(token);
                }
            }
            tokens = notEmptyTokens.toArray(new String[] {});
        }
        if (tokens.length > 1) {
            return tokens[1];
        }
        return null;
    }

    public static String getTableName(TableSyntaxNode tableSyntaxNode) {
        String methodName = StringUtils.EMPTY;
        String[] tokens = REGEX.split(getSignature(tableSyntaxNode));
        if (tokens.length > 3) {
            List<String> notEmptyTokens = new ArrayList<>();
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    notEmptyTokens.add(token);
                }
            }
            tokens = notEmptyTokens.toArray(new String[] {});
        }
        if (tokens != null && tokens.length > 2) {
            int bracketIndex = tokens[2].indexOf('(');
            if (bracketIndex >= 0) {
                methodName = tokens[2].substring(0, bracketIndex);
            } else {
                methodName = tokens[2];
            }
        }
        return methodName;
    }

}
