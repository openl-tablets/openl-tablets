package org.openl.studio.audit.service;

import java.util.Collection;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.stereotype.Component;

import org.openl.rules.rest.acl.service.AclRoleAuditor;
import org.openl.rules.rest.acl.service.AclRoleAuditor.Place;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService.Location;
import org.openl.util.FileUtils;

/**
 * Announces the roles a user gains or loses with the groups the identity provider gives the user or takes away.
 *
 * <p>A role a group holds belongs to every member of the group. The identity provider reports the groups of a user at
 * sign-in: a group reported anew brings the user each role the group holds on all the design or production
 * repositories, on a repository or on a project, and a group no longer reported takes them away. Each such role is
 * announced as granted to the user or revoked from it, naming the group.
 *
 * <p>A role the user holds itself or through another group is not weighed, so an announced role may leave what the
 * user may do as it was.
 */
@Component
@RequiredArgsConstructor
public class ExternalGroupRolesAuditor {

    private final RepositoryAclServiceProvider aclServiceProvider;
    private final SecureDesignTimeRepository designTimeRepository;
    private final AclRoleAuditor roleAuditor;

    /**
     * Announces the roles a user gained with the groups it joined and lost with the groups it left.
     */
    public void announce(String user, Collection<String> joined, Collection<String> left) {
        joined.forEach(group -> announce(user, group, true));
        left.forEach(group -> announce(user, group, false));
    }

    private void announce(String user, String group, boolean joined) {
        var sid = new GrantedAuthoritySid(group);
        for (var repositoryType : AclRepositoryType.values()) {
            var aclService = aclServiceProvider.getAclService(repositoryType.getType());
            aclService.listAllPermissions(sid).forEach((location, permissions) -> {
                var place = placeOf(repositoryType, location);
                var role = AclRoleAuditor.roleOf(permissions);
                if (place.isPresent() && role != null) {
                    roleAuditor.announceThroughGroup(place.get(), user, group, role, joined);
                }
            });
        }
    }

    /**
     * The place a role is held on. A path names a project of a design repository; roles are given on production
     * repositories, never on paths in them.
     */
    private Optional<Place> placeOf(AclRepositoryType repositoryType, Location location) {
        var repositoryId = location.repositoryId();
        if (repositoryId == null) {
            return Optional.of(Place.root(repositoryType));
        }
        var path = location.path();
        if (path == null) {
            return Optional.of(Place.repository(repositoryType, repositoryId));
        }
        return repositoryType == AclRepositoryType.DESIGN
                ? Optional.of(projectOf(repositoryId, path))
                : Optional.empty();
    }

    /**
     * The project stored in a folder of a design repository, named as the project names itself: by its folder, or in
     * a repository that keeps projects in folders of their own, by the name its mapping gives the folder.
     */
    private Place projectOf(String repositoryId, String path) {
        if (designTimeRepository.getRepository(repositoryId) instanceof FolderMapper mapper) {
            var name = Optional.ofNullable(mapper.findMappedName(path)).map(mapper::getBusinessName).orElse(path);
            return Place.project(repositoryId, FileUtils.getName(name), path);
        }
        return Place.project(repositoryId, FileUtils.getName(path), null);
    }
}
