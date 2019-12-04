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
                    continue;
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                default:
                    if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                        String s = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u" + s.substring(s.length() - 4, s.length()));
                    } else {
                        retval.append(ch);
                    }
                    continue;
            }
        }
        return retval.toString();
    }

    @Override
    public void parseTopNode(String type) {
        try {
            if (type.equals("method.body")) {
                parseTopNodeInternal();
            } else if (type.equals("method.header")) {
                parseMethodHeader();
            } else if (type.equals("module")) {
                parseModuleInternal();
            } else if (type.equals("type")) {
                parseType();
            }
        } catch (ParseException pe) {

            SyntaxNodeException sne = reparseTokens(pe);
            if (sne == null) {
                sne = new org.openl.syntax.exception.SyntaxNodeException(pe.getMessage(),
                    null,
                    pos(pe.currentToken),
                    syntaxBuilder.getModule());
            }
            // pe.printStackTrace();
            // throw pe;
            syntaxBuilder.addError(sne);
        } catch (TokenMgrError err) {
            org.openl.util.text.TextInterval loc = new org.openl.util.text.TextInterval(
                pos(0, 0),
                pos(0, 0));

            syntaxBuilder.addError(new org.openl.syntax.exception.SyntaxNodeException(err.getMessage(),
                null,
                loc,
                syntaxBuilder.getModule()));
        } catch (Exception e) {
            syntaxBuilder.addError(
                new org.openl.syntax.exception.SyntaxNodeException("", e, pos(token), syntaxBuilder.getModule()));
        } catch (Throwable t) {
            syntaxBuilder.addError(
                new org.openl.syntax.exception.SyntaxNodeException("", t, pos(token), syntaxBuilder.getModule()));
        }
    }

    private SyntaxNodeException reparseTokens(ParseException pe) {

        BExGrammar be = new BExGrammar();

        be.setModule(syntaxBuilder.getModule());
        be.ReInit(syntaxBuilder.getModule().getCharacterStream());

        BracketMatcher bm = new BracketMatcher();

        while (true) {

            Token t = be.getNextToken();
            if (t.kind == EOF) {
                break;
            }

            BracketMatcher.BracketsStackObject bso = bm.addToken(t.image, t);
            if (bso != null) {
                if (bso.getErrorCode() == BracketMatcher.UNEXPECTED) {
                    String message = String.format("Unexpected bracket '%s'", addEscapes(t.image));

                    return new SyntaxNodeException(message, null, pos(t), syntaxBuilder.getModule());
                }

                if (bso.getErrorCode() == BracketMatcher.MISMATCHED) {
                    Token t2 = (Token) bso.getId();

                    String message = String.format("Mismatched: opened with '%s' and closed with '%s'",
                        addEscapes(t2.image.substring(0, 1)),
                        addEscapes(t.image));
                    return new SyntaxNodeException(message, null, pos(t2, t), syntaxBuilder.getModule());
                }

                throw new RuntimeException("Unknown BracketMatchError = " + bso.getErrorCode());
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
