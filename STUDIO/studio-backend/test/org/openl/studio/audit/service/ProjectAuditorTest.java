package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.security.acl.permission.AclRole;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditSubjectType;
import org.openl.studio.audit.model.AuditTargetType;

class ProjectAuditorTest {

    private final List<AuditEvent> events = new ArrayList<>();
    private final ProjectAuditor auditor = new ProjectAuditor(event -> events.add((AuditEvent) event));

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void created_namesTheProjectItsRepositoryBranchAndRevision() {
        auditor.created(workspaceProject("Pricing", "design", "feature", "rev-2"), "template");

        var event = single(AuditAction.PROJECT_CREATED);
        assertEquals(AuditTargetType.PROJECT, event.targetType());
        assertEquals("Pricing", event.target());
        assertEquals(Map.of("repository", "design", "branch", "feature", "revision", "rev-2", "source", "template"),
                event.details());
    }

    @Test
    void copied_namesTheSource() {
        var source = designProject("Pricing", "design", "master");

        auditor.copied(workspaceProject("Pricing Copy", "design", "master", "rev-3"), source, "rev-1");

        var event = single(AuditAction.PROJECT_COPIED);
        assertEquals("Pricing Copy", event.target());
        assertEquals("Pricing", event.details().get("source"));
        assertEquals("design", event.details().get("sourceRepository"));
        assertEquals("master", event.details().get("sourceBranch"));
        assertEquals("rev-1", event.details().get("sourceRevision"));
    }

    @Test
    void creatorRole_isGrantedToTheSignedInUser() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("jdoe", "secret"));

        auditor.creatorRole(designProject("Pricing", "design", null));

        var event = single(AuditAction.ROLE_GRANTED);
        assertEquals(AuditTargetType.PROJECT, event.targetType());
        assertEquals("Pricing", event.target());
        assertEquals(AuditSubjectType.USER, event.subjectType());
        assertEquals("jdoe", event.subject());
        assertEquals(Map.of("repository", "design", "to", "CONTRIBUTOR"), event.details());
    }

    @Test
    void saved_keepsTheCommentAndLeavesOutWhatIsNotKnown() {
        auditor.saved(workspaceProject("Pricing", "design", null, null), "Rates of 2027", null);

        var event = single(AuditAction.PROJECT_SAVED);
        assertEquals(Map.of("repository", "design", "comment", "Rates of 2027"), event.details());
    }

    @Test
    void deleted_takesTheRolesHeldOnTheProjectAway() {
        var testers = new GrantedAuthoritySid("Testers");

        auditor.deleted(workspaceProject("Pricing", "design", "master", "rev-4"), null,
                Map.of(testers, List.of(AclRole.VIEWER.getCumulativePermission())));

        assertEquals(2, events.size());
        assertEquals(AuditAction.PROJECT_DELETED, events.getFirst().action());
        assertEquals(Map.of("repository", "design", "branch", "master"), events.getFirst().details());
        var revoked = events.getLast();
        assertEquals(AuditAction.ROLE_REVOKED, revoked.action());
        assertEquals("Testers", revoked.subject());
        assertEquals("VIEWER", revoked.details().get("from"));
    }

    @Test
    void unlocked_namesWhoHeldTheLock() {
        auditor.unlocked(workspaceProject("Pricing", "design", "master", null), "jsmith");

        assertEquals("jsmith", single(AuditAction.PROJECT_UNLOCKED).details().get("lockedBy"));
    }

    @Test
    void branchEvents_targetTheRepository() {
        auditor.branchCreated("design", "feature", "master", "Pricing");
        auditor.branchDeleted("design", "feature", "Pricing");
        auditor.merged(workspaceProject("Pricing", "design", "master", null), "feature", "master", true);
        auditor.bypassed(protectedBranch("design", "master"), "master", "Pricing");

        assertEquals(List.of(AuditAction.BRANCH_CREATED, AuditAction.BRANCH_DELETED, AuditAction.BRANCH_MERGED,
                AuditAction.PROTECTION_BYPASSED), events.stream().map(AuditEvent::action).toList());
        assertTrue(events.stream().allMatch(event -> event.targetType() == AuditTargetType.REPOSITORY));
        assertTrue(events.stream().allMatch(event -> "design".equals(event.target())));
        assertEquals(Map.of("branch", "feature", "project", "Pricing", "startPoint", "master"),
                events.getFirst().details());
        assertEquals(Map.of("branch", "master", "project", "Pricing", "sourceBranch", "feature",
                "conflictsResolved", true), events.get(2).details());
        assertEquals(Map.of("branch", "master", "project", "Pricing"), events.getLast().details());
    }

    @Test
    void bypassed_announcesNothingForABranchThatIsNotProtected() {
        var repository = mock(BranchRepository.class);

        auditor.bypassed(repository, "feature", "Pricing");

        assertTrue(events.isEmpty());
    }

    @Test
    void deployed_targetsTheDeployment() {
        auditor.deployed("production", "Pricing Rules", workspaceProject("Pricing", "design", "master", "rev-5"),
                "Release 7");

        var event = single(AuditAction.PROJECT_DEPLOYED);
        assertEquals(AuditTargetType.DEPLOYMENT, event.targetType());
        assertEquals("Pricing Rules", event.target());
        assertEquals(Map.of("repository", "production", "project", "Pricing", "projectRepository", "design",
                "branch", "master", "revision", "rev-5", "comment", "Release 7"), event.details());
    }

    @Test
    void projectsOfARepositoryWithFoldersOfItsOwn_areNamedWithTheirFolders() {
        var project = inFolder(workspaceProject("Pricing", "design", "master", "rev-6"), "sales/Pricing");
        var source = inFolder(workspaceProject("Pricing", "design", "master", "rev-1"), "claims/Pricing");

        auditor.saved(project, null, null);
        auditor.copied(project, source, "rev-1");
        auditor.deployed("production", "Pricing Rules", project, null);

        assertEquals("sales/Pricing", events.getFirst().details().get("path"));
        assertEquals("sales/Pricing", events.get(1).details().get("path"));
        assertEquals("claims/Pricing", events.get(1).details().get("sourcePath"));
        assertEquals("sales/Pricing", events.get(2).details().get("projectPath"));
    }

    @Test
    void failureToRecord_neverFailsTheAction() {
        var failing = new ProjectAuditor(event -> {
            throw new IllegalStateException("The database is gone");
        });

        failing.branchDeleted("design", "feature", "Pricing");

        assertTrue(events.isEmpty());
    }

    private static RulesProject workspaceProject(String name, String repositoryId, String branch, String revision) {
        var project = mock(RulesProject.class);
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repositoryId);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getRepository()).thenReturn(repository);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getBranch()).thenReturn(branch);
        var data = new FileData();
        data.setVersion(revision);
        when(project.getFileData()).thenReturn(data);
        return project;
    }

    private static RulesProject inFolder(RulesProject project, String path) {
        var repository = project.getDesignRepository();
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setMappedFolders(true).build());
        when(project.getRealPath()).thenReturn(path);
        return project;
    }

    private static BranchRepository protectedBranch(String repositoryId, String branch) {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn(repositoryId);
        when(repository.isBranchProtected(branch)).thenReturn(true);
        return repository;
    }

    private static AProject designProject(String name, String repositoryId, String branch) {
        var project = mock(AProject.class);
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn(repositoryId);
        when(repository.getBranch()).thenReturn(branch);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(branch != null).build());
        when(project.getRepository()).thenReturn(repository);
        when(project.getBusinessName()).thenReturn(name);
        return project;
    }

    private AuditEvent single(AuditAction action) {
        assertEquals(1, events.size());
        var event = events.getFirst();
        assertEquals(action, event.action());
        return event;
    }
}
