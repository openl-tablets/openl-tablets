package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TableSyntaxNodeHelper {
    public static String getSignature(TableSyntaxNode rableSyntaxNode) {
        return rableSyntaxNode.getHeader().getHeaderToken().getModule().getCode();
    }

    public static String getTableName(TableSyntaxNode tableSyntaxNode) {
        String methodName = StringUtils.EMPTY;
        String[] tokens = getSignature(tableSyntaxNode).split("[ ,()\t\n\r\\[\\]]");
        if (tokens.length > 3){
            List<String> notEmptyTokens = new ArrayList<String>();
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    notEmptyTokens.add(token);
                }
            }
            tokens = notEmptyTokens.toArray(new String[] {});
        }
        if (tokens != null && tokens.length > 2) {
            int bracketIndex = tokens[2].indexOf("(");
            if (bracketIndex >= 0) {
                methodName = tokens[2].substring(0, bracketIndex);
            } else {
                methodName = tokens[2];
            }
        }
        return methodName;
    }

}
