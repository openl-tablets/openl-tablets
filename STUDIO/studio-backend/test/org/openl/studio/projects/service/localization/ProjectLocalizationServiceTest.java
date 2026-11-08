package org.openl.studio.projects.service.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.Severity;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.Page;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.SortOrder;
import org.openl.studio.projects.model.localization.LocalizationAction;
import org.openl.studio.projects.model.localization.LocalizationKeyView;
import org.openl.studio.projects.model.localization.LocalizationLanguageView;
import org.openl.studio.projects.model.localization.LocalizationSharedValueView;
import org.openl.studio.projects.model.localization.LocalizationWarningView;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.LocalizationFileMessageSource;
import org.openl.studio.projects.model.project.status.MessagePositions;
import org.openl.studio.projects.model.project.status.TextSpot;
import org.openl.studio.projects.service.DetailedMessageDescriptionMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.project.compile.CompilationJob;
import org.openl.studio.projects.service.project.compile.CompilationJobRegistry;
import org.openl.studio.projects.validator.ModifyRestrictedException;

class ProjectLocalizationServiceTest {

    @TempDir
    Path workspace;

    private Path i18n;
    private final RulesProject project = mock(RulesProject.class);
    private final WorkspaceProjectService workspaceProjectService = mock(WorkspaceProjectService.class);
    private final DetailedMessageDescriptionMapper mapper = mock(DetailedMessageDescriptionMapper.class);
    private final CompilationJobRegistry registry = mock(CompilationJobRegistry.class);
    private final FileRoot fileRoot = mock(FileRoot.class);
    private ProjectLocalizationService service;
    private ProjectFileRootFactory fileRoots;

    @BeforeEach
    void setUp() throws IOException {
        var folder = workspace.resolve("main");
        i18n = Files.createDirectories(folder.resolve("i18n"));
        var descriptor = descriptor("main", folder);
        var base = descriptor("base", Files.createDirectories(workspace.resolve("base")));
        Files.createDirectories(base.getProjectFolder().resolve("i18n"));
        Files.writeString(base.getProjectFolder().resolve("i18n/message_de.properties"), "farewell = Tschüss\n");
        when(workspaceProjectService.getProjectDescriptor(project)).thenReturn(descriptor);
        when(workspaceProjectService.localizationDependenciesOf(descriptor)).thenReturn(List.of(base));
        var ids = Map.of(base.getProjectFolder(), "BASE");
        when(workspaceProjectService.dependencyIdsOf(project)).thenReturn(ids);
        when(workspaceProjectService.checkLocalization(descriptor)).thenReturn(List.of());
        fileRoots = mock(ProjectFileRootFactory.class);
        when(fileRoots.of(project)).thenReturn(fileRoot);
        when(registry.find(any(), any())).thenReturn(Optional.empty());
        service = sessionService();
        Files.writeString(i18n.resolve("message.properties"), """
                greeting = Hello, {0}!
                farewell = Bye
                total = Total
                """);
        Files.writeString(i18n.resolve("message_de.properties"), "greeting = Hallo, {0}!\n");
    }

    @Test
    void answersAPageOfTheKeysWithTheirTextsTheSharedOnesAndTheLanguages() {
        warn("greeting", "de", "Something is wrong.");

        var page = service.messages(project, LocalizationQuery.ALL, null, Page.of(0, 2));

        assertEquals(List.of(new LocalizationLanguageView("", null), new LocalizationLanguageView("de", null)),
                page.getLanguages());
        assertEquals(3L, page.getTotal());
        var rows = List.copyOf(page.getContent());
        assertEquals(List.of("greeting", "farewell"), rows.stream().map(row -> row.key()).toList());
        assertEquals(Map.of("", "Hello, {0}!", "de", "Hallo, {0}!"), rows.get(0).values());
        assertEquals(List.of(new LocalizationWarningView("de", "Something is wrong.", List.of(new TextSpot(0, 5)))),
                rows.get(0).warnings());
        assertEquals(Map.of("de", new LocalizationSharedValueView("Tschüss", "base", "BASE")), rows.get(1).shared());
    }

    @Test
    void answersAKeyAloneWhateverTheQuery() throws IOException {
        var query = new LocalizationQuery("nothing", true, List.of(), null);
        var page = service.messages(project, query, "total", Page.of(0, 50));

        assertEquals(List.of("total"), page.getContent().stream().map(row -> row.key()).toList());
        assertTrue(service.messages(project, LocalizationQuery.ALL, "unknown", Page.of(0, 50))
                .getContent()
                .isEmpty());
        // A key only a project it depends on gives is answered with the text it gives.
        Files.writeString(i18n.resolve("message.properties"), "greeting = Hello\n");
        var shared = service.messages(project, LocalizationQuery.ALL, "farewell", Page.of(0, 50)).getContent();
        assertEquals(Map.of(), shared.iterator().next().values());
        assertEquals("base", shared.iterator().next().shared().get("de").project());
    }

    @Test
    void answersEveryKeyForTheTree() {
        warn("total", "", "A warning.");

        assertEquals(keys("greeting", "farewell", "total"), service.keys(project));
    }

    @Test
    void narrowsThePagesDownAndOrdersThemAsTheQuerySays() throws IOException {
        warn("total", "", "A warning.");
        Files.writeString(i18n.resolve("message_fr.properties"), "greeting = Salut\n");

        assertEquals(List.of("greeting"), pageOf(new LocalizationQuery("HALLO", false, List.of(), null)));
        assertEquals(List.of("total"), pageOf(new LocalizationQuery(null, true, List.of(), null)));
        // German takes farewell from the project it depends on: only total shows the default text.
        assertEquals(List.of("total"), pageOf(new LocalizationQuery(null, false, List.of("de"), null)));
        // French shows the default text for farewell and total, German for total alone: total is left by both.
        assertEquals(List.of("farewell", "total"), pageOf(new LocalizationQuery(null, false, List.of("fr"), null)));
        assertEquals(List.of("total"), pageOf(new LocalizationQuery(null, false, List.of("fr", "de"), null)));
        assertEquals(List.of("farewell", "greeting", "total"),
                pageOf(new LocalizationQuery(null, false, List.of(), SortOrder.ASC)));
        assertEquals(List.of("total", "greeting", "farewell"),
                pageOf(new LocalizationQuery(null, false, List.of(), SortOrder.DESC)));
    }

    private List<String> pageOf(LocalizationQuery query) {
        return service.messages(project, query, null, Page.of(0, 50))
                .getContent()
                .stream()
                .map(row -> row.key())
                .toList();
    }

    @Test
    void writesTheChangesInPlaceAndTellsTheCompiledProject() {
        var model = mock(ProjectModel.class);
        var job = mock(CompilationJob.class);
        when(job.project()).thenReturn(model);
        when(registry.find(any(), any())).thenReturn(Optional.of(job));

        service.apply(project, List.of(
                new LocalizationAction.SetValue("greeting", "", "Hi, {0}!"),
                new LocalizationAction.AddKey("note", Map.of("", "A note", "de", "Eine Notiz")),
                new LocalizationAction.RemoveKey("farewell"),
                new LocalizationAction.RemoveValue("greeting", "de"),
                new LocalizationAction.AddLanguage("fr", new LinkedHashMap<>(Map.of("greeting", "Salut, {0}!")))));

        assertEquals("greeting = Hi, {0}!\ntotal = Total\nnote = A note\n", read("message.properties"));
        // The German file had a message written with " = ", which the new one follows; a new file takes "=".
        assertEquals("note = Eine Notiz\n", read("message_de.properties"));
        assertEquals("greeting=Salut, {0}!\n", read("message_fr.properties"));
        verify(fileRoot).requireModifiable();
        verify(project).setModified();
        verify(project).refresh();
        verify(model).checkLocalizationAgain();
        // What is kept is read again: the next page tells the new texts.
        var page = service.messages(project, LocalizationQuery.ALL, "note", Page.of(0, 50));
        assertEquals(Map.of("", "A note", "de", "Eine Notiz"), page.getContent().iterator().next().values());
    }

    @Test
    void refusesALanguageTheEngineWouldNotOpenTheFileOf() {
        // The engine names the file of a locale with the country in upper case: message_de_lu.properties is never
        // read on a file system that tells the cases apart.
        for (var language : List.of("de_lu", "de_LU_", "DE_LU")) {
            var changes = List.<LocalizationAction>of(new LocalizationAction.AddLanguage(language, null));
            assertThrows(BadRequestException.class, () -> service.apply(project, changes), language);
        }
        service.apply(project, List.of(new LocalizationAction.AddLanguage("de_LU", null)));
        assertTrue(Files.exists(i18n.resolve("message_de_LU.properties")));
    }

    @Test
    void writesNothingToAProjectNobodyOpened() {
        when(workspaceProjectService.getProjectDescriptor(project))
                .thenThrow(new ConflictException("project.not.opened.message"));
        var changes = List.<LocalizationAction>of(new LocalizationAction.SetValue("total", "", "Sum"));

        assertThrows(ConflictException.class, () -> service.apply(project, changes));

        assertEquals("greeting = Hello, {0}!\nfarewell = Bye\ntotal = Total\n", read("message.properties"));
        verify(project, never()).setModified();
    }

    @Test
    void writesNothingForAUserWhoMayNotWriteToTheProject() {
        doThrow(new ForbiddenException("default.message")).when(fileRoot).requireModifiable();
        var changes = List.<LocalizationAction>of(new LocalizationAction.SetValue("total", "", "Sum"));

        assertThrows(ForbiddenException.class, () -> service.apply(project, changes));

        assertEquals("greeting = Hello, {0}!\nfarewell = Bye\ntotal = Total\n", read("message.properties"));
        verify(project, never()).setModified();
    }

    @Test
    void writesNothingToAProjectWhoseStateForbidsAChange() throws ProjectException {
        var refused = mock(ModifyRestrictedException.class);
        doThrow(refused).when(fileRoot).requireModifiable();
        var changes = List.<LocalizationAction>of(new LocalizationAction.SetValue("total", "", "Sum"));

        assertSame(refused, assertThrows(ModifyRestrictedException.class, () -> service.apply(project, changes)));

        assertEquals("greeting = Hello, {0}!\nfarewell = Bye\ntotal = Total\n", read("message.properties"));
        // Neither the lock of the project is taken.
        verify(project, never()).tryLockOrThrow();
    }

    @Test
    void refusesAllTheChangesWhenOneIsRefused() {
        var changes = List.<LocalizationAction>of(new LocalizationAction.SetValue("total", "", "Sum"),
                new LocalizationAction.AddKey("greeting", Map.of("", "Again")));

        assertThrows(ConflictException.class, () -> service.apply(project, changes));

        assertEquals("greeting = Hello, {0}!\nfarewell = Bye\ntotal = Total\n", read("message.properties"));
        verify(project, never()).setModified();
    }

    @Test
    void writesOverWhatAnotherSessionWroteAfterThisOneReadTheFiles() {
        service.messages(project, LocalizationQuery.ALL, null, Page.of(0, 50));
        // Another session, with messages of its own kept, writes a line first: the lines of this one move down.
        sessionService().apply(project, List.of(new LocalizationAction.SetValue("greeting", "", "Hello there, {0}!")));

        service.apply(project, List.of(new LocalizationAction.SetValue("total", "", "Sum")));

        assertEquals("greeting = Hello there, {0}!\nfarewell = Bye\ntotal = Sum\n", read("message.properties"));
    }

    @Test
    void writesNoFileWhenOneOfThemCannotBeWritten() throws IOException {
        // Something else than a localization file stands where the new one would be written.
        Files.createDirectory(i18n.resolve("message_fr.properties"));
        var changes = List.<LocalizationAction>of(new LocalizationAction.SetValue("total", "", "Sum"),
                new LocalizationAction.AddLanguage("fr", Map.of("total", "Somme")));

        assertThrows(ConflictException.class, () -> service.apply(project, changes));

        assertEquals("greeting = Hello, {0}!\nfarewell = Bye\ntotal = Total\n", read("message.properties"));
        try (var files = Files.list(i18n)) {
            assertEquals(Set.of("message.properties", "message_de.properties", "message_fr.properties"),
                    files.map(file -> file.getFileName().toString()).collect(Collectors.toSet()));
        }
        verify(project, never()).setModified();
    }

    @Test
    void refusesWhatTheFilesDoNotHold() {
        assertRefused(NotFoundException.class, new LocalizationAction.SetValue("a", "it", "x"));
        assertRefused(NotFoundException.class, new LocalizationAction.RemoveKey("unknown"));
        assertRefused(NotFoundException.class, new LocalizationAction.RemoveValue("total", "de"));
        assertRefused(ConflictException.class, new LocalizationAction.AddLanguage("de", null));
        assertRefused(BadRequestException.class, new LocalizationAction.AddKey("a", Map.of("de-LU", "x")));
    }

    private void assertRefused(Class<? extends Exception> refusal, LocalizationAction action) {
        var actions = List.of(action);
        assertThrows(refusal, () -> service.apply(project, actions));
    }

    @Test
    void refusesAProjectAnotherUserLocked() throws ProjectException {
        doThrow(new ProjectException("locked")).when(project).tryLockOrThrow();

        assertRefused(ConflictException.class, new LocalizationAction.SetValue("total", "", "Sum"));
        assertFalse(read("message.properties").contains("Sum"));
    }

    @Test
    void readsALegacyFileAsSuch() throws IOException {
        Files.write(i18n.resolve("message_fr.properties"), "prix = 5 €\n".getBytes(MessageFile.WINDOWS_1252));

        var page = service.messages(project, LocalizationQuery.ALL, null, Page.of(0, 50));

        assertEquals(new LocalizationLanguageView("fr", true), page.getLanguages().get(2));
        assertNull(page.getLanguages().get(0).legacy());
    }

    /** The service as a session sees it: with the messages of its own kept. */
    private ProjectLocalizationService sessionService() {
        var model = mock(ProjectModel.class);
        when(model.getLocalizationCache()).thenReturn(new LocalizationCache());
        var studio = mock(WebStudio.class);
        when(studio.getModel()).thenReturn(model);
        return new ProjectLocalizationService(workspaceProjectService, mapper, fileRoots, registry) {
            @Override
            public WebStudio getWebStudio() {
                return studio;
            }
        };
    }

    private void warn(String key, String language, String summary) {
        var message = DetailedMessageDescription.builder()
                .source(new MessageDescription(1, summary, Severity.WARN))
                .location(LocalizationFileMessageSource.builder()
                        .path("i18n/message.properties")
                        .key(key)
                        .language(language)
                        .spots(List.of(new TextSpot(0, 5)))
                        .build())
                .build();
        when(mapper.mapFileMessages(any(), any(), any(MessagePositions.class))).thenReturn(List.of(message));
    }

    private static List<LocalizationKeyView> keys(String... keys) {
        return Arrays.stream(keys).map(LocalizationKeyView::new).toList();
    }

    private String read(String file) {
        try {
            return Files.readString(i18n.resolve(file));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ProjectDescriptor descriptor(String name, Path folder) {
        var descriptor = mock(ProjectDescriptor.class);
        when(descriptor.getName()).thenReturn(name);
        when(descriptor.getProjectFolder()).thenReturn(folder);
        return descriptor;
    }
}
