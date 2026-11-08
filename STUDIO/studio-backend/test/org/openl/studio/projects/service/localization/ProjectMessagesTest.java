package org.openl.studio.projects.service.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.ProjectDescriptor;

class ProjectMessagesTest {

    @TempDir
    Path workspace;

    @Test
    void showsWhatALanguageFallsBackToTheProjectsItDependsOnBeforeAParentLanguage() throws IOException {
        var project = project("main", """
                greeting = Hello
                shared = Default
                silent
                """);
        write(project, "de", "greeting = Hallo\n");
        write(project, "de_LU", "");
        var base = project("base", "");
        write(base, "de", "shared = Geteilt\n");

        var messages = new LocalizationCache().messagesOf(project, List.of(base));

        assertEquals(List.of("", "de", "de_LU"), messages.files().stream().map(MessageFile::tag).toList());
        assertEquals(List.of("greeting", "shared", "silent"), messages.keys());
        assertEquals(new ProjectMessages.Shown.Inherited("Hallo", "de", null), messages.shownIn("de_LU", "greeting"));
        assertEquals(new ProjectMessages.Shown.Inherited("Geteilt", "de", "base"),
                messages.shownIn("de_LU", "shared"));
        // A key written without a text stops the search: the rule gets the key itself.
        assertEquals(new ProjectMessages.Shown.Missing(), messages.shownIn("de", "silent"));
        assertEquals(new ProjectMessages.Shown.Own("Hello"), messages.shownIn("", "greeting"));
    }

    @Test
    void leadsAKeyOnlyAProjectItDependsOnGivesToTheNearestOneGivingItADefaultText() throws IOException {
        var project = project("main", "own = Own\n");
        write(project, "fr", "translated = Traduit\n");
        var near = project("near", "");
        write(near, "fr", "shared = Partagé\n");
        var far = project("far", "shared = Shared\nown = Theirs\ntranslated = Translated\n");

        var messages = new LocalizationCache().messagesOf(project, List.of(near, far));

        // The nearest project gives the key in French only: the default text is the farther one's.
        assertEquals(Optional.of(far.getProjectFolder()), messages.ownerOf("shared"));
        // A key the project writes, in any language, is edited where it is.
        assertEquals(Optional.empty(), messages.ownerOf("own"));
        assertEquals(Optional.empty(), messages.ownerOf("translated"));
        assertEquals(Optional.empty(), messages.ownerOf("absent"));
    }

    @Test
    void tellsTheKeysALanguageShowsTheDefaultTextOf() throws IOException {
        var project = project("main", "greeting = Hello\nfarewell = Bye\n");
        write(project, "fr", "greeting = Bonjour\n");

        var messages = new LocalizationCache().messagesOf(project, List.of());

        assertTrue(messages.showsDefaultText("fr", "farewell"));
        assertFalse(messages.showsDefaultText("fr", "greeting"));
        assertFalse(messages.showsDefaultText("", "farewell"));
    }

    @Test
    void readsAgainOnlyWhenAFileChangedOrWasWritten() throws IOException {
        var project = project("main", "greeting = Hello\n");
        var cache = new LocalizationCache();

        var first = cache.messagesOf(project, List.of());
        assertSame(first, cache.messagesOf(project, List.of()));

        write(project, "fr", "greeting = Bonjour\n");
        var withFrench = cache.messagesOf(project, List.of());
        assertNotSame(first, withFrench);
        assertEquals(2, withFrench.files().size());

        cache.forget(project.getProjectFolder().resolve("i18n"));
        assertNotSame(withFrench, cache.messagesOf(project, List.of()));
    }

    @Test
    void asksTheCheckOfTheFilesOnce() throws IOException {
        var messages = new LocalizationCache().messagesOf(project("main", "a = 1\n"), List.of());
        var checks = new int[1];

        messages.problems(() -> {
            checks[0]++;
            return List.of();
        });
        messages.problems(() -> {
            checks[0]++;
            return List.of();
        });

        assertEquals(1, checks[0]);
    }

    private ProjectDescriptor project(String name, String defaultText) throws IOException {
        var folder = workspace.resolve(name);
        Files.createDirectories(folder.resolve("i18n"));
        Files.writeString(folder.resolve("i18n").resolve("message.properties"), defaultText);
        var descriptor = mock(ProjectDescriptor.class);
        when(descriptor.getName()).thenReturn(name);
        when(descriptor.getProjectFolder()).thenReturn(folder);
        return descriptor;
    }

    private static void write(ProjectDescriptor project, String tag, String text) throws IOException {
        Files.writeString(project.getProjectFolder().resolve("i18n").resolve("message_" + tag + ".properties"), text);
    }
}
