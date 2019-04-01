package org.openl.grammar.bexgrammar;

import org.openl.grammar.BracketMatcher;
import org.openl.grammar.ParserErrorMessage;
import org.openl.syntax.exception.SyntaxNodeException;

public class BExGrammarWithParsingHelp extends BExGrammar {

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
            } else if (type.equals("range.literal.real")) {
                RangeLiteralFloat();
            } else if (type.equals("range.literal")) {
                RangeLiteral();
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
                pos(err.getStartLine(), err.getStartCol()),
                pos(err.getEndLine(), err.getEndCol()));

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
                    String message = ParserErrorMessage.printUnexpectedBracket(t.image);

                    return new SyntaxNodeException(message, null, pos(t), syntaxBuilder.getModule());
                }

                if (bso.getErrorCode() == BracketMatcher.MISMATCHED) {
                    Token t2 = (Token) bso.getId();

                    String message = ParserErrorMessage.printMismatchedBracket(t2.image.substring(0, 1), t.image);
                    return new SyntaxNodeException(message, null, pos(t2, t), syntaxBuilder.getModule());
                }

                throw new RuntimeException("Unknown BracketMatchError = " + bso.getErrorCode());
            }

        }

        BracketMatcher.BracketsStackObject bso = bm.checkAtTheEnd();
        if (bso != null) {
            Token t = (Token) bso.getId();

            String message = ParserErrorMessage.printUmatchedBracket(t.image);

            return new SyntaxNodeException(message, null, pos(t), syntaxBuilder.getModule());

        }

        return null;
    }

}
