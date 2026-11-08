package org.openl.rules.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLFileMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.source.SourceRange;
import org.openl.util.TextRange;

class LocalizationCheckTest {

    @TempDir
    Path project;

    @TempDir
    Path dependency;

    private URLClassLoader loader;

    @AfterEach
    void closeLoader() throws IOException {
        if (loader != null) {
            loader.close();
        }
    }

    @Test
    void findsNothingInAProjectWithoutLocalizationFiles() throws IOException {
        Files.createDirectories(project.resolve("i18n"));
        // Only the message files are localization files, and a file whose name holds no language tag is none either.
        Files.writeString(project.resolve("i18n/readme.properties"), "a=b\na=c\n");
        Files.writeString(project.resolve("i18n/message_backup.properties"), "a=b\na=c\n");
        Files.writeString(project.resolve("i18n/message_d.properties"), "a=b\na=c\n");

        var result = check();

        assertFalse(result.files());
        assertTrue(result.messages().isEmpty());
    }

    @Test
    void warnsAboutAFileTheEngineCannotReadRatherThanFailing() throws IOException {
        // A path written with single backslashes reads "\\users" as a broken \\u escape.
        write("message.properties", "path = C:\\users\\me\n");

        var message = single(check().messages());

        assertEquals("A \\u not followed by four hexadecimal digits breaks the file: none of its texts is read. "
                + "Write \\\\u for a backslash before u.", message.getSummary());
        assertEquals(project.resolve("i18n/message.properties"), message.getFile());
    }

    @Test
    void warnsAboutAKeyWrittenTwiceOnTheLineThatWins() throws IOException {
        write("message.properties", "greeting = Hello\nfarewell = Bye\ngreeting = Hi\n");

        var result = check();

        assertTrue(result.files());
        var message = single(result.messages());
        assertEquals("Written twice, on lines 1 and 3: the last text wins.", message.getSummary());
        assertEquals(Severity.WARN, message.getSeverity());
        assertEquals(3, message.getLine());
        assertEquals("greeting", message.getKey());
        assertEquals("", message.getLanguage());
        assertEquals(project.resolve("i18n/message.properties"), message.getFile());
        // An editor of the file marks the key of the line.
        assertEquals(List.of(new TextRange(3, 0, 3, 8)), message.getPlaces());
    }

    @Test
    void warnsAboutALineWithoutASeparatorUnlessADependencyGivesItsText() throws IOException {
        write("message.properties", "greeting = Hello\nonly key\nshared key\n");
        writeDependency("message.properties", "shared key = Shared\n");

        var message = single(check().messages());

        assertEquals("""
                No '=' or ':' on the line, so there is no text and the rule returns the key. Write 'only key =' for \
                an empty text.""", message.getSummary());
        assertEquals("only key", message.getKey());
        assertEquals(2, message.getLine());
    }

    @Test
    void tellsTheSameProblemOfTheSamePieceOnceWithEveryPlaceItIsIn() throws IOException {
        write("message.properties", "total = Total {0,number,0.##} of {1}\n");
        write("message_zh_TW.properties", "total = 總計{0，number，0.##}共{1}\n");

        var messages = check().messages();

        var message = single(messages);
        assertEquals("Full-width '，' instead of ',' in a placeholder. The rule fails when values are passed.",
                message.getSummary());
        assertEquals("zh_TW", message.getLanguage());
        var text = "總計{0，number，0.##}共{1}";
        assertEquals(List.of(4, 11), message.getPieces().stream().map(SourceRange::start).toList());
        message.getPieces().forEach(piece -> {
            assertEquals(text, piece.code());
            assertEquals("，", text.substring(piece.start(), piece.end()));
        });
        assertEquals(message.getPieces().getFirst(), message.getSourceRange());
        // Where the pieces are written in the file.
        assertEquals(List.of(new TextRange(1, 12, 1, 13), new TextRange(1, 19, 1, 20)), message.getPlaces());
    }

    @Test
    void comparesThePlaceholdersOfATranslationWithThoseOfTheDefaultText() throws IOException {
        write("message.properties", "greeting = Hello, {0} and {1}!\n");
        write("message_fr.properties", "greeting = Bonjour, {0} !\n");

        var message = single(check().messages());

        assertEquals("Placeholders [0] differ from [0, 1] of the default language.", message.getSummary());
        assertEquals("fr", message.getLanguage());
    }

    @Test
    void tellsWhatAnApostropheOfTheLanguageDoesToTheText() throws IOException {
        write("message.properties", "price = The price {0} is not known\nitem = Item {0} of offer {1}\n");
        write("message_fr.properties", """
                price = Le prix {0} n'est pas connu
                item = Pour l'article "{0}" et l'offre "{1}"
                """);

        var summaries = check().messages().stream().map(OpenLMessage::getSummary).toList();

        assertEquals(List.of("""
                The single quote in 'est pas connu is dropped when values are passed, so the text is printed without \
                this apostrophe. Use two single quotes ('') to represent an apostrophe.""", """
                The single quotes around 'article "{0}" et l' are dropped when values are passed, and the placeholder \
                between them is printed as written instead of its value. Use two single quotes ('') to represent an \
                apostrophe."""), summaries);
    }

    @Test
    void takesTheDefaultTextOfADependencyForAKeyTheProjectLacks() throws IOException {
        write("message.properties", "greeting = Hello\n");
        write("message_fr.properties", "greeting = Bonjour\nshared = Partagé {0}\norphan = Orphelin\n");
        writeDependency("message.properties", "shared = Shared {0} and {1}\n");

        var messages = check().messages();

        // The dependency gives the default text of 'shared', and with it the placeholders a translation takes.
        assertEquals(List.of(
                "shared: Placeholders [0] differ from [0, 1] of the default language.",
                "orphan: No text in the default language: a language without a text of its own returns the key."),
                messages.stream()
                        .map(OpenLFileMessage.class::cast)
                        .map(message -> message.getKey() + ": " + message.getSummary())
                        .toList());
    }

    @Test
    void countsTheKeysALanguageLeavesUntranslatedOnItsFirstLine() throws IOException {
        write("message.properties", "greeting = Hello\nfarewell = Bye\nthanks = Thanks\nempty\n");
        write("message_de.properties", "greeting = Hallo\n");
        write("message_fr.properties", "greeting = Bonjour\nfarewell = Au revoir\n");
        // A regional language takes what its language translates.
        write("message_fr_CA.properties", "thanks = Merci\n");

        var messages = check().messages()
                .stream()
                .map(OpenLFileMessage.class::cast)
                .filter(message -> message.getKey() == null)
                .toList();

        // A key the default language writes without a text has nothing to translate.
        assertEquals(List.of("de: 2 keys are not translated into German: the default texts are shown.",
                "fr: 1 key is not translated into French: the default text is shown."),
                messages.stream().map(message -> message.getLanguage() + ": " + message.getSummary()).toList());
        messages.forEach(message -> {
            assertEquals(1, message.getLine());
            assertNull(message.getSourceRange());
            assertTrue(message.getPlaces().isEmpty());
        });
    }

    private LocalizationCheck.Result check() throws MalformedURLException {
        loader = new URLClassLoader(new URL[] {project.toUri().toURL(), dependency.toUri().toURL()}, null);
        return LocalizationCheck.check(project, new OpenLMessageSource(loader));
    }

    private void write(String file, String text) throws IOException {
        Files.createDirectories(project.resolve("i18n"));
        Files.writeString(project.resolve("i18n").resolve(file), text);
    }

    private void writeDependency(String file, String text) throws IOException {
        Files.createDirectories(dependency.resolve("i18n"));
        Files.writeString(dependency.resolve("i18n").resolve(file), text);
    }

    private static OpenLFileMessage single(List<OpenLMessage> messages) {
        assertEquals(1, messages.size(), () -> messages.stream().map(OpenLMessage::getSummary).toList().toString());
        return (OpenLFileMessage) messages.getFirst();
    }
}
