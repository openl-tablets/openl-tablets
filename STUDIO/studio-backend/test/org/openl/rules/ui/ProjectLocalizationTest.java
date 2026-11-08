package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLFileMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.binding.MessageCall;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.ISyntaxNode;

class ProjectLocalizationTest {

    @TempDir
    Path folder;

    @Test
    void theFilesOfAProjectAreTheFirstStepOfItsCompilation() throws IOException {
        Files.createDirectories(folder.resolve("i18n"));
        Files.writeString(folder.resolve("i18n/message.properties"), "greeting=Hello\ngreeting=Hi\n");
        var localization = new ProjectLocalization();

        localization.checkFiles(project(), List.of());

        var status = statusOf(localization);
        assertEquals(1, status.getModulesCount());
        assertEquals(1, status.getModulesCompiled());
        assertEquals(1, status.getWarningsCount());
        assertTrue(status.getAllMessage().iterator().next() instanceof OpenLFileMessage);
        assertTrue(localization.isOf(folder));
    }

    @Test
    void theCallsOfEveryModuleCheckedSoFarAreKeptWhicheverCheckEndsLast() throws IOException {
        Files.createDirectories(folder.resolve("i18n"));
        Files.writeString(folder.resolve("i18n/message.properties"), "greeting=Hello\n");
        var localization = new ProjectLocalization();
        localization.checkFiles(project(), List.of());
        var claimsLoader = mock(IDependencyLoader.class);
        var pricingLoader = mock(IDependencyLoader.class);
        var claims = moduleCalling("claims.missing");
        var pricing = moduleCalling("pricing.missing");

        var loaders = List.of(claimsLoader, pricingLoader);
        // Each module that finished compiling is checked, in whatever order the checks end.
        localization.checkCalls(Map.of(pricingLoader, pricing), loaders);
        localization.checkCalls(Map.of(claimsLoader, claims), loaders);
        assertEquals(List.of("claims.missing", "pricing.missing"), missingKeys(localization, claims, pricing));

        // A module compiled again replaces the one its loader compiled before: what was fixed is not told any more.
        var claimsFixed = moduleCalling("greeting");
        localization.checkCalls(Map.of(claimsLoader, claimsFixed), loaders);
        assertEquals(List.of("pricing.missing"), missingKeys(localization, claims, claimsFixed, pricing));

        // A write of the files checks the calls of the modules checked before against the new files.
        Files.writeString(folder.resolve("i18n/message.properties"), "greeting=Hello\npricing.missing=Found\n");
        localization.checkFiles(project(), List.of());
        localization.checkCalls(Map.of(), loaders);
        assertEquals(List.of(), missingKeys(localization, claimsFixed, pricing));
        // The warnings of a module are told with it alone.
        var pricingOther = moduleCalling("pricing.other");
        localization.checkCalls(Map.of(pricingLoader, pricingOther), loaders);
        assertEquals(List.of(), missingKeys(localization, claimsFixed));
        assertEquals(List.of("pricing.other"), missingKeys(localization, pricingOther));

        // A module that left the project is dropped with its warnings.
        localization.checkCalls(Map.of(), List.of(claimsLoader));
        assertEquals(List.of(), missingKeys(localization, pricingOther));
    }

    @Test
    void aProjectWithoutLocalizationFilesAddsNoStep() {
        var localization = new ProjectLocalization();

        localization.checkFiles(project(), List.of());

        var status = statusOf(localization);
        assertEquals(0, status.getModulesCount());
        assertEquals(0, status.getWarningsCount());
    }

    @Test
    void aProjectIsCheckedOnceWithoutBeingCompiled() throws IOException {
        Files.createDirectories(folder.resolve("i18n"));
        Files.writeString(folder.resolve("i18n/message.properties"), "greeting=Hello\ngreeting=Hi\n");

        var result = ProjectLocalization.check(project(), List.of());

        assertTrue(result.files());
        assertEquals(1, result.messages().size());
    }

    @Test
    void whatWasFoundIsForgottenWithTheProject() throws IOException {
        Files.createDirectories(folder.resolve("i18n"));
        Files.writeString(folder.resolve("i18n/message.properties"), "greeting=Hello\ngreeting=Hi\n");
        var localization = new ProjectLocalization();
        localization.checkFiles(project(), List.of());

        localization.clear();

        var status = statusOf(localization);
        assertEquals(0, status.getModulesCount());
        assertEquals(0, status.getWarningsCount());
        assertFalse(localization.isOf(folder));
    }

    private ProjectDescriptor project() {
        var project = new ProjectDescriptor();
        project.setName("Rating");
        project.setProjectFolder(folder);
        return project;
    }

    private static ProjectCompilationStatus statusOf(ProjectLocalization localization) {
        var status = ProjectCompilationStatus.newBuilder();
        localization.addTo(status);
        return status.build();
    }

    /** A module calling {@code msg} with a key, and with nothing else to check. */
    private static XlsModuleOpenClass moduleCalling(String key) {
        var module = mock(XlsModuleOpenClass.class);
        when(module.getMessageCalls()).thenReturn(List.of(new MessageCall(key, mock(ISyntaxNode.class),
                mock(ISyntaxNode.class), 0)));
        return module;
    }

    /** The keys the warnings about the calls of the modules name, in order. */
    private static List<String> missingKeys(ProjectLocalization localization, XlsModuleOpenClass... modules) {
        return localization.callMessages(List.of(modules))
                .stream()
                .map(OpenLMessage::getSummary)
                .map(summary -> summary.split("'")[1])
                .sorted()
                .toList();
    }
}
