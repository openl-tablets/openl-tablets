package org.openl.j;

import org.openl.grammar.bexgrammar.BExGrammar;
import org.openl.grammar.bexgrammar.ParseException;
import org.openl.grammar.bexgrammar.Token;
import org.openl.grammar.bexgrammar.TokenMgrError;
import org.openl.syntax.exception.SyntaxNodeException;

public class BExGrammarWithParsingHelp extends BExGrammar {

    private static String addEscapes(String str) {
        StringBuilder retval = new StringBuilder();
        char ch;
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case 0:
                    break;
                case '\b':
                    retval.append("\\b");
                    break;
                case '\t':
                    retval.append("\\t");
                    break;
                case '\n':
                    retval.append("\\n");
                    break;
                case '\f':
                    retval.append("\\f");
                    break;
                case '\r':
                    retval.append("\\r");
                    break;
                default:
                    if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                        String s = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u").append(s.substring(s.length() - 4));
                    } else {
                        retval.append(ch);
                    }
            }
        }
        return retval.toString();
    }

    @Override
    public void parseTopNode(String type) {
        try {
            switch (type) {
                case "method.body":
                    parseTopNodeInternal();
                    break;
                case "method.header":
                    parseMethodHeader();
                    break;
                case "module":
                    parseModuleInternal();
                    break;
                case "type":
                    parseType();
                    break;
            }
        } catch (ParseException pe) {

            SyntaxNodeException sne = reparseTokens();
            if (sne == null) {
                sne = new org.openl.syntax.exception.SyntaxNodeException(pe.getMessage(),
                    null,
                    pos(pe.currentToken),
                    syntaxBuilder.getModule());
            }
            syntaxBuilder.addError(sne);
        } catch (TokenMgrError err) {
            StringBuilder buf = new StringBuilder();
            org.openl.util.text.TextInterval loc = pos(err.getMessage(), token, buf);

            syntaxBuilder.addError(new org.openl.syntax.exception.SyntaxNodeException(err.getMessage(),
                null,
                loc,
                syntaxBuilder.getModule()));
        } catch (Throwable e) {
            syntaxBuilder.addError(
                new SyntaxNodeException("", e, pos(token), syntaxBuilder.getModule()));
        }
    }

    private SyntaxNodeException reparseTokens() {

        BExGrammar be = new BExGrammar();

        be.setModule(syntaxBuilder.getModule());
        be.ReInit(syntaxBuilder.getModule().getCharacterStream());

        BracketMatcher bm = new BracketMatcher();

        while (true) {

            Token t;
            try {
                t = be.getNextToken();
            } catch (TokenMgrError err) {
                StringBuilder buf = new StringBuilder();
                org.openl.util.text.TextInterval loc = pos(err.getMessage(), token, buf);

                return new SyntaxNodeException(err.getMessage(),
                    null,
                    loc,
                    syntaxBuilder.getModule());
            }
            if (t.kind == EOF) {
                break;
            }

            BracketMatcher.BracketsStackObject bso = bm.addToken(t.image, t);
            if (bso != null) {
                String message;
                switch (bso.getErrorCode()) {
                    case UNEXPECTED:
                        message = String.format("Unexpected bracket '%s'", addEscapes(t.image));

                        return new SyntaxNodeException(message, null, pos(t), syntaxBuilder.getModule());
                    case MISMATCHED:
                        Token t2 = (Token) bso.getId();

                        message = String.format("Mismatched: opened with '%s' and closed with '%s'",
                            addEscapes(t2.image.substring(0, 1)),
                            addEscapes(t.image));
                        return new SyntaxNodeException(message, null, pos(t2, t), syntaxBuilder.getModule());
                    case UNMATCHED:
                        throw new IllegalStateException("UNMATCHED error type shouldn't appear here");
                    default:
                        throw new IllegalStateException("Unknown BracketMatchError = " + bso.getErrorCode());
                }

            }

        }

        BracketMatcher.BracketsStackObject bso = bm.checkAtTheEnd();
        if (bso != null) {
            Token t = (Token) bso.getId();

            String message = String.format("Need to close '%s'", addEscapes(t.image));

            return new SyntaxNodeException(message, null, pos(t), syntaxBuilder.getModule());

        }

        return null;
    }

}
