package org.openl.studio.transfer.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.ProjectId;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.converter.ProjectIdConverter;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.session.ClientSessions;
import org.openl.studio.transfer.ClientTransferLinks;
import org.openl.studio.transfer.TransferLinks;
import org.openl.studio.transfer.TransferOperation;
import org.openl.studio.transfer.model.TransferKind;
import org.openl.studio.transfer.model.TransferLinkRequest;

class TransferLinksControllerTest {

    private final TransferLinks links = new TransferLinks(
            Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC), "PT15M", 20);
    private final ClientSessions sessions = mock(ClientSessions.class);
    private final ProjectIdConverter projects = mock(ProjectIdConverter.class);
    private final TransferLinksController controller = new TransferLinksController(new ClientTransferLinks(links),
            sessions, projects);

    @BeforeEach
    void signIn() {
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("jdoe", null, List.of()));
        when(sessions.currentKey()).thenReturn("jdoe|pat:1");
        var project = mock(RulesProject.class);
        when(project.getProjectId()).thenReturn(new ProjectId("CURRENT"));
        when(projects.convert("OLD")).thenReturn(project);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aLinkIsIssuedForTheProjectUnderItsCurrentId() {
        var view = controller.create(request(TransferKind.DOWNLOAD, null));

        assertTrue(view.path().startsWith("transfers/"));
        assertEquals(Instant.parse("2026-10-23T10:15:00Z"), view.expiresAt());
        var link = links.find(view.path().substring("transfers/".length()));
        assertEquals("jdoe", link.user());
        assertEquals("jdoe|pat:1", link.clientKey());
        assertEquals(new TransferOperation.Download("CURRENT", "rules/Rules.xlsx", "main", "abc", false),
                link.operation());
    }

    @Test
    void eachKindRecordsItsOperation() {
        assertEquals(new TransferOperation.Download("CURRENT", "rules/Rules.xlsx", "main", "abc", true),
                operationOf(request(TransferKind.ARCHIVE, null)));
        assertEquals(new TransferOperation.ConflictVersion("CURRENT", "rules/Rules.xlsx", ConflictBase.OURS),
                operationOf(request(TransferKind.CONFLICT_VERSION, ConflictBase.OURS)));
        assertEquals(new TransferOperation.Upload("CURRENT", "rules/Rules.xlsx", "main", ConflictPolicy.FAIL, true,
                null, null), operationOf(request(TransferKind.UPLOAD, null)));
        assertEquals(new TransferOperation.Upload("CURRENT", "rules/Rules.xlsx", null, ConflictPolicy.OVERWRITE,
                false, 10L, "ab".repeat(32)), operationOf(new TransferLinkRequest(TransferKind.UPLOAD, "OLD",
                "rules/Rules.xlsx", null, null, null, ConflictPolicy.OVERWRITE, false, 10L, "ab".repeat(32))));
    }

    @Test
    void aConflictVersionNeedsItsSide() {
        var request = request(TransferKind.CONFLICT_VERSION, null);

        assertThrows(BadRequestException.class, () -> controller.create(request));
    }

    @Test
    void aPathLeadingOutOfTheProjectIsRefused() {
        for (var path : List.of("../users/info", "rules/../../x", "/rules.xml", "rules//a.xml")) {
            var request = new TransferLinkRequest(TransferKind.UPLOAD, "OLD", path, null, null, null, null, null,
                    null, null);

            assertThrows(BadRequestException.class, () -> controller.create(request), path);
        }
    }

    @Test
    void aBrowserSessionGetsNoLink() {
        when(sessions.currentKey()).thenReturn(null);
        var request = request(TransferKind.DOWNLOAD, null);

        assertThrows(BadRequestException.class, () -> controller.create(request));
    }

    private TransferOperation operationOf(TransferLinkRequest request) {
        var view = controller.create(request);
        return links.find(view.path().substring("transfers/".length())).operation();
    }

    private static TransferLinkRequest request(TransferKind kind, ConflictBase side) {
        return new TransferLinkRequest(kind, "OLD", "rules/Rules.xlsx", "main", "abc", side, null, null, null, null);
    }
}
