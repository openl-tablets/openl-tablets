package org.openl.rules.repository.git.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.repository.git.text.TextMerge.Conflict;
import org.openl.rules.repository.git.text.TextMerge.Merged;

class TextMergeTest {

    private static final String BASE = "key1=value1\nkey2=value2\nkey3=value3\nkey4=value4\nkey5=value5\n";

    @Test
    void changesApartAreMergedByThemselvesEachWithTheLinesOfEveryVersion() {
        var blocks = merge(BASE, BASE.replace("value1", "ours"), BASE.replace("value5", "theirs"));

        assertEquals(List.of(
                new Merged("key1=ours\n", "key1=ours\n", "key1=value1\n", "key1=value1\n"),
                new Merged("key2=value2\nkey3=value3\nkey4=value4\n"),
                new Merged("key5=theirs\n", "key5=value5\n", "key5=value5\n", "key5=theirs\n")), blocks);
        assertTrue(((Merged) blocks.getFirst()).changed());
        assertFalse(((Merged) blocks.get(1)).changed());
    }

    @Test
    void lineBothBranchesChangeIsAConflictBetweenTheLinesMerged() {
        var blocks = merge(BASE, BASE.replace("value3", "ours"), BASE.replace("value3", "theirs"));

        assertEquals(List.of(
                new Merged("key1=value1\nkey2=value2\n"),
                new Conflict("key3=ours\n", "key3=value3\n", "key3=theirs\n"),
                new Merged("key4=value4\nkey5=value5\n")), blocks);
    }

    @Test
    void linesBothBranchesAppendAreAConflictWithNoBase() {
        var blocks = merge(BASE, BASE + "key6=ours\n", BASE + "key6=theirs\n");

        assertEquals(List.of(new Merged(BASE), new Conflict("key6=ours\n", "", "key6=theirs\n")), blocks);
    }

    @Test
    void linesKeepTheirLineBreaks() {
        var base = BASE.replace("\n", "\r\n");
        var ours = base.replace("value3", "ours");
        var blocks = merge(base, ours, base.replace("value3", "theirs"));

        // Taking our version of every conflict gives our file back, byte for byte.
        var text = new StringBuilder();
        blocks.forEach(block -> text.append(switch (block) {
            case Merged merged -> merged.text();
            case Conflict conflict -> conflict.ours();
        }));
        assertEquals(ours, text.toString());
    }

    @Test
    void lastLineWithoutALineBreakIsALineToo() {
        var blocks = merge("a\nb", "a\nours", "a\ntheirs");

        assertEquals(List.of(new Merged("a\n"), new Conflict("ours", "b", "theirs")), blocks);
    }

    @Test
    void fileBothBranchesAddDifferentlyIsOneConflict() {
        var blocks = merge("", "ours\n", "theirs\n");

        assertEquals(List.of(new Conflict("ours\n", "", "theirs\n")), blocks);
    }

    @Test
    void lineOneBranchRemovesAndTheOtherChangesIsAConflict() {
        var blocks = merge(BASE, BASE.replace("key3=value3\n", ""), BASE.replace("value3", "theirs"));

        assertEquals(List.of(
                new Merged("key1=value1\nkey2=value2\n"),
                new Conflict("", "key3=value3\n", "key3=theirs\n"),
                new Merged("key4=value4\nkey5=value5\n")), blocks);
    }

    @Test
    void lineBothBranchesChangeAlikeIsMergedByItself() {
        var both = BASE.replace("value3", "both");

        assertEquals(List.of(
                new Merged("key1=value1\nkey2=value2\n"),
                new Merged("key3=both\n", "key3=both\n", "key3=value3\n", "key3=both\n"),
                new Merged("key4=value4\nkey5=value5\n")), merge(BASE, both, both));
    }

    @Test
    void blocksBothBranchesAddAtOnePlaceStayWholeThoughTheyStartAndEndAlike() {
        var base = """
                <modules>
                    <module>
                        <name>RULES</name>
                    </module>
                </modules>
                """;
        var ours = base.replace("</modules>", """
                    <module>
                        <name>projectB</name>
                    </module>
                </modules>""");
        var theirs = base.replace("</modules>", """
                    <module>
                        <name>projectA</name>
                    </module>
                </modules>""");

        // Taking both versions declares both modules, each whole.
        assertEquals(List.of(
                new Merged("<modules>\n    <module>\n        <name>RULES</name>\n    </module>\n"),
                new Conflict("    <module>\n        <name>projectB</name>\n    </module>\n",
                        "",
                        "    <module>\n        <name>projectA</name>\n    </module>\n"),
                new Merged("</modules>\n")), merge(base, ours, theirs));
    }

    @Test
    void conflictHoldsTheLinesBothBranchesShareAtItsEdgesAndTheBaseTheyReplaced() {
        var blocks = merge("a\nold\nb\n", "a\nbegin\nours\nend\nb\n", "a\nbegin\ntheirs\nend\nb\n");

        assertEquals(List.of(
                new Merged("a\n"),
                new Conflict("begin\nours\nend\n", "old\n", "begin\ntheirs\nend\n"),
                new Merged("b\n")), blocks);
    }

    @Test
    void contentThatIsNoUtf8TextIsNotMerged() {
        var text = bytes(BASE);
        var binary = new byte[] {'a', 0, 'b', '\n'};
        var latin1 = "Prämie=1\n".getBytes(StandardCharsets.ISO_8859_1);

        assertTrue(TextMerge.merge(text, text, binary).isEmpty());
        assertTrue(TextMerge.merge(latin1, text, text).isEmpty());
    }

    @Test
    void contentIsToldTextWhenItIsUtf8WithNoNulCharacterInItsFirstBytes() throws IOException {
        // Text longer than the first bytes binary content is told by, so that the rest is read as well.
        var longText = BASE.repeat(1000);
        var latin1 = (longText + "Prämie=1\n").getBytes(StandardCharsets.ISO_8859_1);

        assertTrue(TextMerge.isText(new ByteArrayInputStream(bytes(longText))));
        assertTrue(TextMerge.isText(new ByteArrayInputStream(new byte[0])));
        assertFalse(TextMerge.isText(new ByteArrayInputStream(new byte[] {'a', 0, 'b', '\n'})));
        assertFalse(TextMerge.isText(new ByteArrayInputStream(latin1)));
        // A NUL character beyond the first bytes does not make content binary, for Git either.
        assertTrue(TextMerge.isText(new ByteArrayInputStream(bytes(longText + "\u0000"))));
        // Nor does a carriage return alone, as old Mac files end their lines with.
        assertTrue(TextMerge.isText(new ByteArrayInputStream(bytes("a=1\rb=2\r"))));
    }

    @Test
    void linesEndedByACarriageReturnAloneAreMergedAsText() {
        var blocks = TextMerge.merge(bytes("a\rb\n"), bytes("a\rours\n"), bytes("a\rtheirs\n")).orElseThrow();

        // A carriage return alone does not end a line, as Git reads lines.
        assertEquals(List.of(new Conflict("a\rours\n", "a\rb\n", "a\rtheirs\n")), blocks);
    }

    private static List<TextMerge.Block> merge(String base, String ours, String theirs) {
        return TextMerge.merge(bytes(base), bytes(ours), bytes(theirs)).orElseThrow();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
