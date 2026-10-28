package org.openl.grammar.bexgrammar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.lang.xls.Parser;
import org.openl.source.impl.StringSourceCodeModule;

class IdentifierTokenTest {

    private static BExGrammarTokenManager tokenManager(String text) {
        return new BExGrammarTokenManager(new SimpleCharStream(new StringReader(text), 1, 1, 16));
    }

    private static Token firstToken(String text) {
        return tokenManager(text).getNextToken();
    }

    private static boolean isIdentifier(String text) {
        try {
            var token = firstToken(text);
            return token.kind == BExGrammarConstants.IDENTIFIER && token.image.equals(text);
        } catch (TokenMgrError e) {
            return false;
        }
    }

    /**
     * Every character of the Basic Multilingual Plane, which the character classes of the grammar cover, and a few
     * characters outside of it, which reach the lexer as pairs of surrogates.
     */
    @Test
    void followsJavaIdentifierRules() {
        var supplementary = IntStream.of(0x1F600, 0x1D7D9, 0x50000, 0xF0000, 0xE0001, 0x10FFFF, 0x1D465, 0x20000,
                0xE0100);
        IntStream.concat(IntStream.rangeClosed(Character.MIN_VALUE, Character.MAX_VALUE), supplementary).forEach(c -> {
            var ch = Character.toString(c);
            assertEquals(Character.isJavaIdentifierStart(c), isIdentifier(ch), () -> "Start U+%04X".formatted(c));
            assertEquals(Character.isJavaIdentifierPart(c), isIdentifier("a" + ch), () -> "Part U+%04X".formatted(c));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"면간거리", "名前", "なまえ", "имя", "όνομα", "שם", "اسم", "नाम", "€rate", "á", "𝑥𝟙", "구간1",
            "`× ÷`"})
    void acceptsIdentifiersOfAnyScript(String name) {
        assertEquals(name, firstToken(name).image);
    }

    @Test
    void parsesKoreanRuleHeader() {
        var code = new Parser().parseAsMethodHeader(new StringSourceCodeModule("Double 면간거리(FinalCode FC)", null));
        assertEquals(0, code.getErrors().length);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "면간×거리 | Lexical error at line 1, column 3.  Encountered: '\\u00d7' (215),",
            "a\ud835 | Lexical error at line 1, column 2.  Encountered: '\\ud835' (55349),",
            "$a×:$b | Lexical error at line 1, column 3.  Encountered: '\\u00d7' (215),",
            "$a:$́b | Lexical error at line 1, column 5.  Encountered: '\\u0301' (769),"})
    void reportsFirstInvalidCharacter(String text, String message) {
        assertEquals(message, assertThrows(TokenMgrError.class, () -> firstToken(text)).getMessage());
    }

    @Test
    void reportsInvalidStartInLexicalState() {
        var manager = tokenManager("a.́");
        manager.getNextToken();
        manager.getNextToken();
        var e = assertThrows(TokenMgrError.class, manager::getNextToken);
        assertEquals("Lexical error at line 1, column 3.  Encountered: '\\u0301' (769), (in lexical state 1)",
                e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"$면:$간격", "$`a b`:$간격", "$a:$`× ÷`"})
    void acceptsRangeVariables(String text) {
        var token = firstToken(text);
        assertEquals(BExGrammarConstants.RANGE_VARIABLE, token.kind);
        assertEquals(text, token.image);
    }
}
