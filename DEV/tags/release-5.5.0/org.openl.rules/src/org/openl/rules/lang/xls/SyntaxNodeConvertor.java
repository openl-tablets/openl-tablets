package org.openl.rules.lang.xls;

import org.openl.syntax.ISyntaxNode;
import org.openl.util.AStringConvertor;

public class SyntaxNodeConvertor extends AStringConvertor<ISyntaxNode> {

    @Override
    public String getStringValue(ISyntaxNode syntaxNode) {

        return syntaxNode.getType();
    }
}
