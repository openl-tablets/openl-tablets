package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;

/**
 * The projects a fill of the tags changed are announced in one event, which never fails the fill.
 *
 * @author Yury Molchan
 */
class TagAuditorTest {

    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final TagAuditor auditor = new TagAuditor(publisher);

    @Test
    void announcesEveryFilledProjectWithItsTagsInOneEvent() {
        var projects = new LinkedHashMap<RulesProject, Map<String, String>>();
        projects.put(project("Policy-Auto", "design", "Policy-Auto"), Map.of("Domain", "Policy", "LOB", "Auto"));
        projects.put(project("Claim-Home", "design", "Claim-Home"), Map.of("Domain", "Claim"));

        auditor.filled(projects);

        var announced = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publishEvent(announced.capture());
        var event = announced.getValue();
        assertEquals(AuditAction.TAGS_FILLED, event.action());
        assertEquals(AuditTargetType.PROJECT, event.targetType());
        assertEquals("Policy-Auto, Claim-Home", event.target());
        assertEquals(Map.of("projects", Map.of(
                "Policy-Auto", Map.of("Domain", "Policy", "LOB", "Auto"),
                "Claim-Home", Map.of("Domain", "Claim"))), event.details());
    }

    @Test
    void tellsApartTheFilledProjectsOfOneNameByTheirRepositoryAndFolder() {
        var projects = new LinkedHashMap<RulesProject, Map<String, String>>();
        projects.put(project("Rating", "design", "rating/Auto"), Map.of("LOB", "Auto"));
        projects.put(project("Rating", "design2", null), Map.of("LOB", "Home"));
        projects.put(project("Claim", "design", "Claim"), Map.of("Domain", "Claim"));

        auditor.filled(projects);

        var announced = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publishEvent(announced.capture());
        var event = announced.getValue();
        assertEquals("Rating (design:/rating/Auto), Rating (design2), Claim", event.target());
        assertEquals(Map.of("projects", Map.of(
                "Rating (design:/rating/Auto)", Map.of("LOB", "Auto"),
                "Rating (design2)", Map.of("LOB", "Home"),
                "Claim", Map.of("Domain", "Claim"))), event.details());
    }

    @Test
    void keepsAProjectNamedAsAnotherOneIsToldApart() {
        var projects = new LinkedHashMap<RulesProject, Map<String, String>>();
        projects.put(project("Rating (design2)", "design", "odd"), Map.of("LOB", "Life"));
        projects.put(project("Rating", "design", "rating/Auto"), Map.of("LOB", "Auto"));
        projects.put(project("Rating", "design2", null), Map.of("LOB", "Home"));

        auditor.filled(projects);

        var announced = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publishEvent(announced.capture());
        assertEquals(Map.of("projects", Map.of(
                "Rating (design2)", Map.of("LOB", "Life"),
                "Rating (design:/rating/Auto)", Map.of("LOB", "Auto"),
                "Rating (design2) (design2:/Rating)", Map.of("LOB", "Home"))), announced.getValue().details());
    }

    @Test
    void tellsApartALocalProjectThatIsInNoDesignRepository() {
        var local = mock(RulesProject.class);
        when(local.getBusinessName()).thenReturn("Rating");
        when(local.isLocalOnly()).thenReturn(true);
        when(local.getRealPath()).thenReturn("Rating");
        var projects = new LinkedHashMap<RulesProject, Map<String, String>>();
        projects.put(local, Map.of("LOB", "Life"));
        projects.put(project("Rating", "design", "rating/Auto"), Map.of("LOB", "Auto"));

        auditor.filled(projects);

        var announced = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publishEvent(announced.capture());
        assertEquals("Rating (local:/Rating), Rating (design:/rating/Auto)", announced.getValue().target());
    }

    @Test
    void announcesNothingWhenNoProjectWasFilled() {
        auditor.filled(Map.of());

        verifyNoInteractions(publisher);
    }

    @Test
    void doesNotFailTheFillWhenTheEventCannotBeRecorded() {
        doThrow(new IllegalStateException("The database is down")).when(publisher).publishEvent(any(AuditEvent.class));

        var project = project("Policy-Auto", "design", "Policy-Auto");

        assertDoesNotThrow(() -> auditor.filled(Map.of(project, Map.of("Domain", "Policy"))));
    }

    /** A project of a design repository, kept in a folder of its own when the path is given, else under its name. */
    private static RulesProject project(String name, String repositoryId, @Nullable String path) {
        var features = mock(Features.class);
        when(features.mappedFolders()).thenReturn(path != null);
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repositoryId);
        when(repository.supports()).thenReturn(features);
        var project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getRealPath()).thenReturn(path == null ? name : path);
        return project;
    }
}
