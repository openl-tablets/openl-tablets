package org.openl.studio.audit.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.rest.acl.service.AclRoleAuditor;
import org.openl.rules.rest.acl.service.AclRoleAuditor.Place;
import org.openl.security.acl.permission.AclRole;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;

/**
 * Announces the activity on projects to the audit log.
 *
 * <p>It covers the projects created, copied, saved, deleted, unlocked and deployed, and the branches of their
 * repositories created, deleted, merged and bypassed. It also covers the role the creator of a project gets on it
 * and the roles that go with a deleted project.
 *
 * <p>A project lives in its repository, which no transaction covers, so an action is announced once it is done. A
 * failure to record it is logged and never fails the action: the action is done already.
 *
 * <p>An event of a project names its design repository and its branch, and its folder in a repository that can hold
 * several projects of one name. The services that act on projects are constructed with this bean.
 */
@Slf4j
@Component
public final class ProjectAuditor {

    // The details the events of several actions carry.
    private static final String BRANCH = "branch";
    private static final String COMMENT = "comment";
    /** The detail that names the project an action on a branch, a bypass or a deployment was made from. */
    static final String PROJECT = "project";
    private static final String REPOSITORY = "repository";
    private static final String REVISION = "revision";
    private static final String SOURCE = "source";
    private static final String SOURCE_BRANCH = "sourceBranch";

    private final ApplicationEventPublisher eventPublisher;
    private final AclRoleAuditor roleAuditor;

    public ProjectAuditor(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.roleAuditor = new AclRoleAuditor(eventPublisher);
    }

    /**
     * Announces a project created.
     *
     * @param source what the project was made of: {@code template}, {@code files} or {@code archive}
     */
    public void created(AProject project, String source) {
        quietly(AuditAction.PROJECT_CREATED, () -> {
            var details = detailsOf(project);
            details.put(REVISION, revisionOf(project));
            details.put(SOURCE, source);
            announce(AuditAction.PROJECT_CREATED, project, details);
        });
    }

    /**
     * Announces a project created as a copy of another one.
     *
     * @param sourceRevision the revision of the source that was copied, or {@code null} for its latest one
     */
    public void copied(AProject project, AProject source, @Nullable String sourceRevision) {
        quietly(AuditAction.PROJECT_COPIED, () -> {
            var details = detailsOf(project);
            details.put(REVISION, revisionOf(project));
            details.put(SOURCE, source.getBusinessName());
            details.put("sourceRepository", repositoryOf(source));
            details.put("sourcePath", Place.pathOf(source));
            details.put(SOURCE_BRANCH, branchOf(source));
            details.put("sourceRevision", sourceRevision);
            announce(AuditAction.PROJECT_COPIED, project, details);
        });
    }

    /**
     * Announces the CONTRIBUTOR role the signed-in user got on a project by creating it.
     */
    public void creatorRole(AProject project) {
        quietly(AuditAction.ROLE_GRANTED, () -> {
            var creator = new PrincipalSid(SecurityContextHolder.getContext().getAuthentication());
            roleAuditor.announce(Place.project(project), creator, null, AclRole.CONTRIBUTOR.name());
        });
    }

    /**
     * Announces the changes of a project saved to its design repository.
     *
     * @param source what the changes came from: {@code null} for the editor, {@code archive} for an uploaded
     *               archive, {@code conflictResolution} for the files chosen to resolve the conflicts of a save
     */
    public void saved(AProject project, @Nullable String comment, @Nullable String source) {
        quietly(AuditAction.PROJECT_SAVED, () -> {
            var details = detailsOf(project);
            details.put(REVISION, revisionOf(project));
            details.put(COMMENT, comment);
            details.put(SOURCE, source);
            announce(AuditAction.PROJECT_SAVED, project, details);
        });
    }

    /**
     * Announces a project deleted, and the roles that were held on it when its access rights went with it.
     *
     * <p>A deleted project has no folder in its repository any more, so the folder it had is told by the caller. The
     * events name it as the other events of the project do.
     *
     * @param path               the folder the project had before it was deleted, as {@link Place#pathOf(AProject)}
     *                           told it, or {@code null} when the repository tells its projects apart by name
     * @param removedPermissions the permissions held on the project, by subject, when its access rights were removed;
     *                           empty when they were kept
     */
    public void deleted(AProject project,
                        @Nullable String path,
                        @Nullable String comment,
                        Map<Sid, List<Permission>> removedPermissions) {
        quietly(AuditAction.PROJECT_DELETED, () -> {
            var details = detailsOf(project, path);
            details.put(COMMENT, comment);
            announce(AuditAction.PROJECT_DELETED, project, details);
            var place = Place.project(project.getRepository().getId(), project.getBusinessName(), path);
            roleAuditor.announce(place, AclRoleAuditor.rolesOf(removedPermissions), Map.of());
        });
    }

    /**
     * Announces the lock of another user released by force.
     *
     * @param lockedBy the user who held the lock
     */
    public void unlocked(AProject project, String lockedBy) {
        quietly(AuditAction.PROJECT_UNLOCKED, () -> {
            var details = detailsOf(project);
            details.put("lockedBy", lockedBy);
            announce(AuditAction.PROJECT_UNLOCKED, project, details);
        });
    }

    /**
     * Announces a branch created in a design repository.
     *
     * @param startPoint the branch or the revision the new branch starts from, or {@code null} for the current branch
     *                   of the repository
     * @param project    the project the branch was created for, or {@code null} when it was created for a project
     *                   still to be made
     */
    public void branchCreated(String repositoryId, String branch, @Nullable String startPoint, @Nullable String project) {
        quietly(AuditAction.BRANCH_CREATED, () -> {
            var details = branchDetails(branch, project);
            details.put("startPoint", startPoint);
            announceBranch(AuditAction.BRANCH_CREATED, repositoryId, details);
        });
    }

    /**
     * Announces a branch deleted from a design repository, with every project it held.
     *
     * @param project the project the branch was deleted from
     */
    public void branchDeleted(String repositoryId, String branch, String project) {
        quietly(AuditAction.BRANCH_DELETED,
                () -> announceBranch(AuditAction.BRANCH_DELETED, repositoryId, branchDetails(branch, project)));
    }

    /**
     * Announces a branch merged into another one of the repository of a project.
     *
     * @param conflictsResolved whether the merge was committed with the files chosen to resolve its conflicts
     */
    public void merged(AProject project, String sourceBranch, String targetBranch, boolean conflictsResolved) {
        quietly(AuditAction.BRANCH_MERGED, () -> {
            var details = branchDetails(targetBranch, project.getBusinessName());
            details.put(SOURCE_BRANCH, sourceBranch);
            details.put("conflictsResolved", conflictsResolved ? Boolean.TRUE : null);
            announceBranch(AuditAction.BRANCH_MERGED, repositoryOf(project), details);
        });
    }

    /**
     * Announces the protection of a branch bypassed by a user allowed to bypass it, who confirmed it.
     *
     * <p>It is announced once the change the bypass let through is made, so a change that fails records none. A
     * change made to a branch that is not protected needed no bypass and announces nothing.
     *
     * @param repository the repository of the branch changed
     * @param project    the name of the project the change was made for
     */
    public void bypassed(BranchRepository repository, String branch, String project) {
        quietly(AuditAction.PROTECTION_BYPASSED, () -> {
            if (repository.isBranchProtected(branch)) {
                announceBranch(AuditAction.PROTECTION_BYPASSED, repository.getId(), branchDetails(branch, project));
            }
        });
    }

    /**
     * Announces a project deployed to a production repository.
     *
     * @param repositoryId   the production repository
     * @param deploymentName the deployment the project went into
     * @param comment        the comment of the deployment, or {@code null} when there is none
     */
    public void deployed(String repositoryId, String deploymentName, AProject project, @Nullable String comment) {
        quietly(AuditAction.PROJECT_DEPLOYED, () -> {
            var details = new LinkedHashMap<String, @Nullable Object>();
            details.put(REPOSITORY, repositoryId);
            details.put(PROJECT, project.getBusinessName());
            details.put("projectRepository", repositoryOf(project));
            details.put("projectPath", Place.pathOf(project));
            details.put(BRANCH, branchOf(project));
            details.put(REVISION, revisionOf(project));
            details.put(COMMENT, comment);
            announce(AuditAction.PROJECT_DEPLOYED, AuditTargetType.DEPLOYMENT, deploymentName, details);
        });
    }

    private void announce(AuditAction action, AProject project, Map<String, @Nullable Object> details) {
        announce(action, AuditTargetType.PROJECT, project.getBusinessName(), details);
    }

    private void announceBranch(AuditAction action, String repositoryId, Map<String, @Nullable Object> details) {
        announce(action, AuditTargetType.REPOSITORY, repositoryId, details);
    }

    private void announce(AuditAction action,
                          AuditTargetType targetType,
                          String target,
                          Map<String, @Nullable Object> details) {
        eventPublisher.publishEvent(AuditEvent.builder()
                .action(action)
                .targetType(targetType)
                .target(target)
                .details(details)
                .build());
    }

    /**
     * Runs an announcement, logging a failure instead of passing it on: the action it announces is done already.
     */
    private static void quietly(AuditAction action, Runnable announcement) {
        try {
            announcement.run();
        } catch (RuntimeException e) {
            log.error("Failed to record the {} event in the audit log.", action, e);
        }
    }

    private static Map<String, @Nullable Object> detailsOf(AProject project) {
        return detailsOf(project, Place.pathOf(project));
    }

    private static Map<String, @Nullable Object> detailsOf(AProject project, @Nullable String path) {
        var details = new LinkedHashMap<String, @Nullable Object>();
        details.put(REPOSITORY, repositoryOf(project));
        details.put("path", path);
        details.put(BRANCH, branchOf(project));
        return details;
    }

    private static Map<String, @Nullable Object> branchDetails(String branch, @Nullable String project) {
        var details = new LinkedHashMap<String, @Nullable Object>();
        details.put(BRANCH, branch);
        details.put(PROJECT, project);
        return details;
    }

    private static String repositoryOf(AProject project) {
        return project instanceof UserWorkspaceProject workspaceProject
                ? workspaceProject.getDesignRepository().getId()
                : project.getRepository().getId();
    }

    private static @Nullable String branchOf(AProject project) {
        if (project instanceof UserWorkspaceProject workspaceProject) {
            return workspaceProject.getBranch();
        }
        // A secured repository is a branch repository by type even when the repository it guards has no branches.
        var repository = project.getRepository();
        return repository.supports().branches() ? ((BranchRepository) repository).getBranch() : null;
    }

    private static @Nullable String revisionOf(AProject project) {
        return Optional.ofNullable(project.getFileData()).map(FileData::getVersion).orElse(null);
    }
}
