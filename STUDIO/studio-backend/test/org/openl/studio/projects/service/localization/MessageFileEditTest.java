package org.openl.studio.projects.service.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.studio.common.exception.ConflictException;

class MessageFileEditTest {

    @TempDir
    Path folder;

    @Test
    void writesATextOverTheKeyWhereItStandsAndKeepsTheRestAsWritten() throws IOException {
        var path = write("""
                # Greetings
                greeting   =   Hello
                multi = one \\
                    two
                farewell : Bye
                """);
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.set("multi", "one and two");
        writeOver(edit);

        assertEquals("""
                # Greetings
                greeting   =   Hello
                multi = one and two
                farewell : Bye
                """, read(path));
    }

    @Test
    void keepsTheSeparatorOfEachLineAndTakesThatOfTheFileForANewKey() throws IOException {
        var path = write("""
                greeting   =   Hello
                farewell: Bye
                silent
                """);
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.set("greeting", "Hi");
        edit.set("farewell", "See you");
        edit.set("silent", "Quiet");
        edit.set("added", "New");
        writeOver(edit);

        // A line written without a separator, and a new line, take the one of the last line written with one.
        assertEquals("""
                greeting   =   Hi
                farewell: See you
                silent: Quiet
                added: New
                """, read(path));
    }

    @Test
    void addsAKeyAtTheEndWithTheLineBreakOfTheFile() throws IOException {
        var path = write("a = 1\r\nb = 2");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.set("c", "3");
        edit.set("b", "two");
        writeOver(edit);

        assertEquals("a = 1\r\nb = two\r\nc = 3\r\n", read(path));
    }

    @Test
    void writesOnlyTheNewLinesWhenKeysAreOnlyAdded() throws IOException {
        var path = write("a = 1\n");
        var before = Files.getLastModifiedTime(path);
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.set("b", "2");
        writeOver(edit);

        assertEquals("a = 1\nb = 2\n", read(path));
        assertTrue(Files.getLastModifiedTime(path).compareTo(before) >= 0);
    }

    @Test
    void takesEveryLineOfAKeyOutAKeyWrittenTwiceIncluded() throws IOException {
        var path = write("a = 1\nb = 2\na = 3\nc = 4\n");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.remove("a");
        assertFalse(edit.has("a"));
        writeOver(edit);

        assertEquals("b = 2\nc = 4\n", read(path));
    }

    @Test
    void takesAKeyOutWithTheLineBreaksOfItsLinesLeavingNoBlankLine() throws IOException {
        var path = write("# head\r\na = 1, \\\r\n    2, \\\r\n    3\r\n# over b\r\nb = 2\r\nc = 3");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.remove("a");
        edit.remove("c");
        writeOver(edit);

        assertEquals("# head\r\n# over b\r\nb = 2\r\n", read(path));
    }

    @Test
    void writesOverTheLastOfAKeyWrittenTwice() throws IOException {
        var path = write("a = 1\na = 2\n");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.set("a", "3");
        writeOver(edit);

        assertEquals("a = 1\na = 3\n", read(path));
    }

    @Test
    void addsAKeyTakenOutAgainAtTheEnd() throws IOException {
        var path = write("a = 1\nb = 2\n");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.remove("a");
        edit.set("a", "again");
        writeOver(edit);

        assertEquals("b = 2\na = again\n", read(path));
    }

    @Test
    void escapesWhatReadingWouldDropOrSplit() throws IOException {
        var path = write("");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.set("a=b: c", " two\nlines ");
        edit.set("#not a comment", "x\\y");
        writeOver(edit);

        var written = MessageFile.read(path, "");
        assertEquals(" two\nlines ", written.texts().get("a=b: c"));
        assertEquals("x\\y", written.texts().get("#not a comment"));
    }

    @Test
    void endsTheLastLineBeforeAddingAKey() throws IOException {
        var path = write("a = 1");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        edit.set("b", "2");
        writeOver(edit);

        assertEquals("a = 1\nb = 2\n", read(path));
    }

    @Test
    void writesAFileInTheLegacyEncodingWholeInUtf8() throws IOException {
        var path = folder.resolve("message_fr.properties");
        Files.write(path, "prix = 5 €\nb = 2\n".getBytes(MessageFile.WINDOWS_1252));
        var edit = new MessageFileEdit(MessageFile.read(path, "fr"));

        edit.set("b", "deux");
        writeOver(edit);

        assertEquals("prix = 5 €\nb = deux\n", read(path));
    }

    @Test
    void createsANewFileWithWhatIsAddedToIt() throws IOException {
        var path = folder.resolve("i18n").resolve("message_de.properties");
        var edit = new MessageFileEdit(path);

        assertTrue(edit.changed());
        edit.set("greeting", "Hallo");
        writeOver(edit);

        assertEquals("greeting=Hallo\n", read(path));
    }

    @Test
    void writesNothingWhenNothingChanged() throws IOException {
        var path = write("a = 1\n");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));

        assertFalse(edit.changed());
    }

    @Test
    void refusesToWriteOverAFileChangedSinceItWasRead() throws IOException {
        var path = write("a = 1\n");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));
        edit.set("a", "2");
        Files.writeString(path, "a = 1\nb = 2\n");

        assertThrows(ConflictException.class, edit::writeAside);

        assertEquals("a = 1\nb = 2\n", read(path));
        assertEquals(List.of(path), listed());
    }

    @Test
    void refusesToWriteOverAFileChangedInTheSameTickToTheSameSize() throws IOException {
        var path = write("a = 1\n");
        var read = MessageFile.read(path, "");
        var edit = new MessageFileEdit(read);
        edit.set("a", "3");
        // Another writer changes the text, keeping the size and the time of the last change of the file.
        Files.writeString(path, "a = 2\n");
        Files.setLastModifiedTime(path, FileTime.fromMillis(read.stamp().modified()));

        assertThrows(ConflictException.class, edit::writeAside);
        assertEquals("a = 2\n", read(path));
    }

    @Test
    void refusesToCreateAFileCreatedSinceTheChangesWereMade() throws IOException {
        var edit = new MessageFileEdit(folder.resolve("message_de.properties"));
        edit.set("a", "1");
        var created = write("message_de.properties", "a = 0\n");

        assertThrows(ConflictException.class, edit::writeAside);
        assertEquals("a = 0\n", read(created));
    }

    @Test
    void leavesTheFileAsItWasUntilTheFileWrittenAsideTakesItsPlace() throws IOException {
        var path = write("# head\na = 1\nb = 2\n");
        var edit = new MessageFileEdit(MessageFile.read(path, ""));
        edit.set("b", "3");

        var aside = edit.writeAside();
        assertEquals("# head\na = 1\nb = 2\n", read(path));
        aside.discard();
        assertEquals(List.of(path), listed());

        var again = new MessageFileEdit(MessageFile.read(path, ""));
        again.set("b", "3");
        writeOver(again);
        assertEquals("# head\na = 1\nb = 3\n", read(path));
        assertEquals(List.of(path), listed());
    }

    @Test
    void putsBackTheFilesReplacedBeforeAFileThatCannotTakeItsPlace() throws IOException {
        var first = write("message.properties", "greeting = Hello\n");
        var second = write("message_fr.properties", "greeting = Bonjour\n");
        var english = new MessageFileEdit(MessageFile.read(first, ""));
        var french = new MessageFileEdit(MessageFile.read(second, "fr"));
        english.set("greeting", "Hi");
        french.set("greeting", "Salut");
        var asides = List.of(english.writeAside(), french.writeAside());
        // Something else takes the place of the French file before it is replaced.
        Files.delete(second);
        Files.createDirectories(second.resolve("taken"));

        assertThrows(IOException.class, () -> MessageFileEdit.replaceAll(asides));

        assertEquals("greeting = Hello\n", read(first));
        asides.forEach(MessageFileEdit.Aside::discard);
        // Neither a file written aside nor a copy kept is left behind.
        assertEquals(List.of(first, second), listed().stream().sorted().toList());
    }

    @Test
    void replacesEveryFileAndKeepsNoCopy() throws IOException {
        var first = write("message.properties", "greeting = Hello\n");
        var english = new MessageFileEdit(MessageFile.read(first, ""));
        english.set("greeting", "Hi");

        MessageFileEdit.replaceAll(List.of(english.writeAside()));

        assertEquals("greeting = Hi\n", read(first));
        assertEquals(List.of(first), listed());
    }

    private static void writeOver(MessageFileEdit edit) throws IOException {
        MessageFileEdit.replaceAll(List.of(edit.writeAside()));
    }

    private List<Path> listed() throws IOException {
        try (var files = Files.list(folder)) {
            return files.toList();
        }
    }

    private Path write(String name, String text) throws IOException {
        var path = folder.resolve(name);
        Files.writeString(path, text);
        return path;
    }

    private Path write(String text) throws IOException {
        var path = folder.resolve("message.properties");
        Files.writeString(path, text);
        return path;
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
