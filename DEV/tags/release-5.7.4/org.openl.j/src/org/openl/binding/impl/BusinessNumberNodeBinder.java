package org.openl.binding.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.LiteralNode;

public abstract class BusinessNumberNodeBinder extends ANodeBinder {

    private static Map<Character, Integer> multiplierSuffixes = new HashMap<Character, Integer>();

    static {
        multiplierSuffixes.put('K', 1000);
        multiplierSuffixes.put('M', 1000 * 1000);
        multiplierSuffixes.put('B', 1000 * 1000 * 1000);
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        assert node instanceof LiteralNode;

        String literal = ((LiteralNode) node).getImage();

        // FIXME: System locals are not supportable
        if (literal.charAt(0) == '$') {
            literal = literal.substring(1);
        }

        // FIXME: System locals are not supportable
        if (literal.indexOf(',') >= 0) {
            literal = literal.replace(",", "");
        }

        int literalLength = literal.length();

        char lastCharacter = Character.toUpperCase(literal.charAt(literalLength - 1));

        IBoundNode parsedNumber = null;

        if (multiplierSuffixes.containsKey(lastCharacter)) {
            String literalWithoutSuffix = literal.substring(0, literalLength - 1);
            parsedNumber = makeNumber(literalWithoutSuffix, multiplierSuffixes.get(lastCharacter), node);
        } else {
            parsedNumber = makeNumber(literal, 1, node);
        }

        return parsedNumber;
    }

    protected abstract IBoundNode makeNumber(String literal, int multiplier, ISyntaxNode node) throws SyntaxNodeException;
}
