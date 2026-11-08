package org.openl.rules.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MessagePatternTest {

    /** A text, the numbers of its placeholders, and each problem of it as its kind and its piece of the text. */
    static Stream<Arguments> patterns() {
        return Stream.of(
                Arguments.of("Le pas {0,number,0.##%}", Set.of(0), List.of()),
                Arguments.of("{0,number,#,##0.00} et {1,number,$#,##0.00}", Set.of(0, 1), List.of()),
                Arguments.of("The value is {0, NUMBER}", Set.of(0), List.of()),
                Arguments.of("Plain text", Set.of(), List.of()),
                // A full-width sign MessageFormat takes as written is no problem.
                Arguments.of("Le pas {０,number,0.##%}", Set.of(0), List.of()),
                Arguments.of("{0,number,'。'0}", Set.of(0), List.of()),
                // A full-width sign is told where it is, and the parts are judged as the translator meant them.
                Arguments.of("最小值為{0，數字，0。##%}", Set.of(0),
                        List.of("FULL_WIDTH[，]", "PLACEHOLDER_TYPE[數字]", "FULL_WIDTH[，]", "FULL_WIDTH[。]")),
                Arguments.of("Le pas {0，number，0.##%}", Set.of(0), List.of("FULL_WIDTH[，]", "FULL_WIDTH[，]")),
                Arguments.of("Le pas {0,number,0。##%}", Set.of(0), List.of("FULL_WIDTH[。]")),
                Arguments.of("Le pas {0、number、0.##}", Set.of(0), List.of("FULL_WIDTH[、]", "FULL_WIDTH[、]")),
                Arguments.of("Le pas {0,數字,0.##%}", Set.of(0), List.of("PLACEHOLDER_TYPE[數字]")),
                Arguments.of("{0,numéro,0.##}", Set.of(0), List.of("PLACEHOLDER_TYPE[numéro]")),
                Arguments.of("{0,number,0.0.0}", Set.of(0), List.of("PLACEHOLDER_FORMAT[0.0.0]")),
                Arguments.of("{0,number,##0#}", Set.of(0), List.of("PLACEHOLDER_FORMAT[##0#]")),
                Arguments.of("{0,date,kurz} {1,date,yyyy-MM-dd} {2,date,yyyy-MM-ddq}", Set.of(0, 1, 2),
                        List.of("PLACEHOLDER_FORMAT[kurz]", "PLACEHOLDER_FORMAT[yyyy-MM-ddq]")),
                // A choice without a choice is taken by MessageFormat, and fails only once a value is formatted.
                Arguments.of("{0,choice,0#none|1#one} {1,choice,abc}", Set.of(0, 1),
                        List.of("PLACEHOLDER_FORMAT[abc]")),
                Arguments.of("{0,choice} {1,date}", Set.of(0, 1), List.of("PLACEHOLDER_FORMAT[choice]")),
                // The text a choice chooses is formatted with the same values: its placeholders are the message's.
                Arguments.of("{0,choice,0#none|1<{1} items}", Set.of(0, 1), List.of()),
                Arguments.of("The value is {0", Set.of(), List.of("BRACE[{0]")),
                Arguments.of("A } and a { alone", Set.of(), List.of("BRACE[{ alone]")),
                Arguments.of("The value is {name}", Set.of(), List.of("PLACEHOLDER_NUMBER[name]")),
                Arguments.of("The value is { 0}", Set.of(), List.of("PLACEHOLDER_NUMBER[ 0]")),
                // Apostrophes matter only once values are passed, that is in a message with a placeholder.
                Arguments.of("Can't do it, can't you see {0}", Set.of(0), List.of("APOSTROPHE['t do it, can']")),
                Arguments.of("Don't pay {0}", Set.of(), List.of("OPEN_APOSTROPHE['t pay {0}]")),
                Arguments.of("l'étape {0,numéro}", Set.of(), List.of("OPEN_APOSTROPHE['étape {0,numéro}]")),
                // An apostrophe after the last placeholder takes only itself away.
                Arguments.of("Le tarif {0} n'est pas connu", Set.of(0), List.of("TRAILING_APOSTROPHE['est pas connu]")),
                // The apostrophe shifts the quotes after it: the one left open before a sign alone tells nothing more.
                Arguments.of("Dans l'étape $'{'\"{0}\"'}'", Set.of(),
                        List.of("APOSTROPHE['étape $']", "PLACEHOLDER_NUMBER['\"{0}\"']")),
                // Apostrophes of the language around a placeholder print it as it is written.
                Arguments.of("Pour l'article \"{0}\" et l'offre \"{1}\"", Set.of(1),
                        List.of("QUOTED_PLACEHOLDER['article \"{0}\" et l']")),
                // A quote meant to print a brace is fine.
                Arguments.of("Quoted '{0}' braces", Set.of(), List.of()),
                Arguments.of("Show '{' and '}' around {0}", Set.of(0), List.of()),
                // A message without a placeholder is printed as written, so a pair of apostrophes shows as two.
                Arguments.of("Can''t be changed", Set.of(), List.of("DOUBLED_APOSTROPHE['']")),
                Arguments.of("l''homme et l''eau", Set.of(),
                        List.of("DOUBLED_APOSTROPHE['']", "DOUBLED_APOSTROPHE['']")));
    }

    @ParameterizedTest
    @MethodSource("patterns")
    void readsThePlaceholdersAndTellsEachProblemWhereItIs(String text, Set<Integer> indices, List<String> problems) {
        var pattern = MessagePattern.of(text);

        assertEquals(indices, pattern.indices());
        assertEquals(problems, pattern.problems()
                .stream()
                .map(problem -> problem.kind() + "[" + text.substring(problem.start(), problem.end()) + "]")
                .toList());
    }

    @Test
    void readsThePlaceholdersOnlyOfAPatternNothingHides() {
        assertTrue(MessagePattern.of("Hello, {0}!").readsPlaceholders());
        // A pair of apostrophes hides no placeholder: it only prints as two.
        assertTrue(MessagePattern.of("Can''t be changed").readsPlaceholders());
        assertFalse(MessagePattern.of("Don't pay {0}").readsPlaceholders());
        assertFalse(MessagePattern.of("The value is {0").readsPlaceholders());
    }

    @Test
    void writesEachFullWidthSignAsItsAsciiOneInItsPlace() {
        assertEquals("{0, number,0.##%}", MessagePattern.asciiOf("{0，　number、0。##％}"));
        assertEquals("Plain 数字", MessagePattern.asciiOf("Plain 数字"));
    }
}
