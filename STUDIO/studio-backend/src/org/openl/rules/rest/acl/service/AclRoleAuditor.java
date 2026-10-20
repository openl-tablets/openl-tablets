package org.openl.rules.rest.acl.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditSubjectType;
import org.openl.studio.audit.model.AuditTargetType;

/**
 * Announces the roles granted, changed and revoked to the audit log.
 *
 * <p>A role is held by a user or a group on all the design or production repositories, on a repository or on a
 * project. A caller reads the role a subject held before a change, makes the change and announces both roles; a role
 * that stays as it was announces nothing.
 *
 * <p>A role a user gains or loses with a group the identity provider gives or takes away is announced for the user,
 * naming the group it comes with.
 */
@Component
@RequiredArgsConstructor
public class AclRoleAuditor {

    private static final String FROM = "from";
    private static final String TO = "to";

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Where a role is held, as the audit log names it.
     *
     * @param type    the kind of the place
     * @param name    the name of the place
     * @param details what else tells the place apart, such as the repository of a project
     */
    public record Place(AuditTargetType type, String name, Map<String, String> details) {

        /** All the design or all the production repositories. */
        public static Place root(AclRepositoryType repositoryType) {
            return new Place(AuditTargetType.ROOT, repositoryType.getType(), Map.of());
        }

        /** A design or a production repository. */
        public static Place repository(AclRepositoryType repositoryType, String repositoryId) {
            return new Place(AuditTargetType.REPOSITORY, repositoryId, Map.of("type", repositoryType.getType()));
        }

        /**
         * A project of a design repository. A repository that can hold several projects of one name names the folder
         * of the project too.
         */
        public static Place project(AProject project) {
            return project(project.getRepository().getId(), project.getBusinessName(), pathOf(project));
        }

        /**
         * A project of a design repository by its name, and by its folder in a repository that can hold several
         * projects of one name.
         *
         * @param path the folder of the project, or {@code null} when the repository tells its projects apart by name
         */
        public static Place project(String repositoryId, String name, @Nullable String path) {
            var details = new LinkedHashMap<String, String>();
            details.put("repository", repositoryId);
            Optional.ofNullable(path).ifPresent(folder -> details.put("path", folder));
            return new Place(AuditTargetType.PROJECT, name, details);
        }

        /**
         * The folder of a project in a design repository that keeps projects in folders of their own, such as a Git
         * repository without the flat folder structure. Such a repository can hold several projects of one name.
         *
         * @return the path of the folder, or {@code null} when the repository tells its projects apart by name
         */
        public static @Nullable String pathOf(AProject project) {
            var repository = project instanceof UserWorkspaceProject workspaceProject
                    ? workspaceProject.getDesignRepository()
                    : project.getRepository();
            return repository.supports().mappedFolders() ? project.getRealPath().replace('\\', '/') : null;
        }
    }

    /**
     * Announces the change of the role a subject holds on a place.
     *
     * @param before the role before the change, or {@code null} when the subject held none
     * @param after  the role after the change, or {@code null} when the subject holds none
     */
    public void announce(Place place, Sid sid, @Nullable String before, @Nullable String after) {
        if (Objects.equals(before, after)) {
            return;
        }
        var action = switch (before) {
            case null -> AuditAction.ROLE_GRANTED;
            default -> after == null ? AuditAction.ROLE_REVOKED : AuditAction.ROLE_CHANGED;
        };
        var details = new LinkedHashMap<String, Object>(place.details());
        details.put(FROM, before);
        details.put(TO, after);
        var subjectType = sid instanceof PrincipalSid ? AuditSubjectType.USER : AuditSubjectType.GROUP;
        publish(action, place, subjectType, nameOf(sid), details);
    }

    /**
     * Announces a role a user gained or lost with a group the identity provider gave the user or took away at
     * sign-in.
     *
     * @param role   the role the group holds on the place
     * @param joined whether the user joined the group, and gained the role, or left it, and lost the role
     */
    public void announceThroughGroup(Place place, String user, String group, String role, boolean joined) {
        var details = new LinkedHashMap<String, Object>(place.details());
        details.put(joined ? TO : FROM, role);
        details.put("group", group);
        details.put(AuditEvent.ORIGIN, AuditEvent.IDENTITY_PROVIDER);
        publish(joined ? AuditAction.ROLE_GRANTED : AuditAction.ROLE_REVOKED, place, AuditSubjectType.USER, user,
                details);
    }

    private void publish(AuditAction action,
                         Place place,
                         AuditSubjectType subjectType,
                         String subject,
                         Map<String, Object> details) {
        eventPublisher.publishEvent(AuditEvent.builder()
                .action(action)
                .targetType(place.type())
                .target(place.name())
                .subjectType(subjectType)
                .subject(subject)
                .details(details)
                .build());
    }

    /**
     * Announces every role on a place that a change granted, changed or revoked.
     *
     * @param before the roles on the place before the change, by subject
     * @param after  the roles on the place after the change, by subject
     */
    public void announce(Place place, Map<Sid, String> before, Map<Sid, String> after) {
        var subjects = new LinkedHashSet<>(before.keySet());
        subjects.addAll(after.keySet());
        subjects.forEach(sid -> announce(place, sid, before.get(sid), after.get(sid)));
    }

    /**
     * The role the permissions of a subject make, or {@code null} when there are none. Permissions that make no
     * known role are named by their mask.
     */
    public static @Nullable String roleOf(Collection<Permission> permissions) {
        if (permissions.isEmpty()) {
            return null;
        }
        return permissions.stream()
                .map(permission -> Optional.ofNullable(AclRole.getRole(permission.getMask()))
                        .map(AclRole::name)
                        .orElseGet(() -> "mask " + permission.getMask()))
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * The roles the permissions of every subject make.
     */
    public static Map<Sid, String> rolesOf(Map<Sid, List<Permission>> permissions) {
        var roles = new LinkedHashMap<Sid, String>();
        permissions.forEach((sid, granted) -> {
            var role = roleOf(granted);
            if (role != null) {
                roles.put(sid, role);
            }
        });
        return roles;
    }

    private static String nameOf(Sid sid) {
        return switch (sid) {
            case PrincipalSid principal -> principal.getPrincipal();
            case GrantedAuthoritySid authority -> authority.getGrantedAuthority();
            default -> sid.toString();
        };
    }
}
