package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping("/acl/repo")
@Hidden
public class RepositoryAclServiceController {

    private static final String REPO_TYPE_PROD = "prod";
    private static final String REPO_TYPE_DESIGN = "design";

    private final RepositoryAclService repositoryAclService;
    private final SimpleRepositoryAclService productionRepositoryAclService;
    private final DeploymentManager deploymentManager;

    private final UserDao userDao;
    private final GroupDao groupDao;

    public RepositoryAclServiceController(UserDao userDao,
            GroupDao groupDao,
            RepositoryAclService repositoryAclService,
            @Qualifier("productionRepositoryAclService") SimpleRepositoryAclService productionRepositoryAclService,
            DeploymentManager deploymentManager) {
        this.repositoryAclService = repositoryAclService;
        this.userDao = userDao;
        this.groupDao = groupDao;
        this.productionRepositoryAclService = productionRepositoryAclService;
        this.deploymentManager = deploymentManager;
    }

    private static Map<String, Map<String, String[]>> convert(Map<Sid, List<Permission>> permissions) {
        Map<String, Map<String, String[]>> ret = new HashMap<>();
        Map<String, String[]> usersPermissions = new HashMap<>();
        Map<String, String[]> groupsPermissions = new HashMap<>();
        for (Map.Entry<Sid, List<Permission>> entry : permissions.entrySet()) {
            String sid;
            boolean principal;
            if (entry.getKey() instanceof PrincipalSid) {
                PrincipalSid principalSid = (PrincipalSid) entry.getKey();
                principal = true;
                sid = principalSid.getPrincipal();
            } else if (entry.getKey() instanceof GrantedAuthoritySid) {
                GrantedAuthoritySid grantedAuthoritySid = (GrantedAuthoritySid) entry.getKey();
                principal = false;
                sid = grantedAuthoritySid.getGrantedAuthority();
            } else {
                throw new IllegalStateException("Unsupported sid type.");
            }
            String[] permissionsArray = entry.getValue().stream().map(e -> {
                AclPermission projectArtifactPermission = AclPermission.getPermission(e.getMask());
                if (projectArtifactPermission != null) {
                    return AclPermission.toString(projectArtifactPermission);
                } else {
                    return String.valueOf(e.getMask());
                }
            }).toArray(String[]::new);
            if (principal) {
                usersPermissions.put(sid, permissionsArray);
            } else {
                groupsPermissions.put(sid, permissionsArray);
            }
        }
        if (!usersPermissions.isEmpty()) {
            ret.put("users", usersPermissions);
        }
        if (!groupsPermissions.isEmpty()) {
            ret.put("groups", groupsPermissions);
        }
        return ret;
    }

    private static List<Permission> buildPermissions(String repositoryType, String[] permissions) {
        if (permissions == null) {
            return Collections.emptyList();
        }
        List<Permission> permissionsList = new ArrayList<>();
        for (String permission : permissions) {
            AclPermission aclPermission = AclPermission.getPermission(permission);
            if (aclPermission == null) {
                throw new NotFoundException("repository.permission.message", permission);
            }
            if (REPO_TYPE_PROD.equals(repositoryType) && !AclPermission.EDIT.equals(aclPermission)) {
                throw new NotFoundException("repository.permission.message", permission);
            }
        }
        return permissionsList;
    }

    private SimpleRepositoryAclService getRepositoryAclService(String repositoryType) {
        if (REPO_TYPE_PROD.equals(repositoryType)) {
            return productionRepositoryAclService;
        } else if (REPO_TYPE_DESIGN.equals(repositoryType)) {
            return repositoryAclService;
        }
        throw new NotFoundException("repository.type.message", repositoryType);
    }

    private void validateRepositoryId(HttpSession session, String repositoryType, String repositoryId) {
        if (REPO_TYPE_DESIGN.equals(repositoryType)) {
            UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);
            if (userWorkspace.getDesignTimeRepository()
                .getRepositories()
                .stream()
                .noneMatch(e -> Objects.equals(e.getId(), repositoryId)) && !Objects.equals(
                    userWorkspace.getDesignTimeRepository().getDeployConfigRepository().getId(),
                    repositoryId)) {
                throw new NotFoundException("repository.message", repositoryId);
            }
        } else if (REPO_TYPE_PROD.equals(repositoryType)) {
            if (deploymentManager.getRepositoryConfigNames().stream().noneMatch(e -> Objects.equals(e, repositoryId))) {
                throw new NotFoundException("repository.message", repositoryId);
            }
        } else {
            throw new NotFoundException("repository.type.message", repositoryType);
        }
    }

    @GetMapping(value = { "/{repositoryType:^design|prod$}/{repo-id}", "{repositoryType:^design|prod$}" })
    public Map<String, Map<String, String[]>> list(@PathVariable("repositoryType") String repositoryType,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        Map<Sid, List<Permission>> permissions;
        SimpleRepositoryAclService simpleRepositoryAclService = getRepositoryAclService(repositoryType);
        if (StringUtils.isBlank(repositoryId)) {
            permissions = simpleRepositoryAclService.listRootPermissions();
        } else {
            validateRepositoryId(session, repositoryType, repositoryId);
            permissions = simpleRepositoryAclService.listPermissions(repositoryId, path);
        }
        return convert(permissions);
    }

    @PutMapping(value = { "/{repositoryType:^design|prod$}/{repo-id}/user/{sid}",
            "/{repositoryType:^design|prod$}/user/{sid}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addUserPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        if (userDao.existsByName(sid)) {
            if (StringUtils.isBlank(repositoryId)) {
                getRepositoryAclService(repositoryType)
                    .addRootPermissions(buildPermissions(repositoryType, permissions), List.of(new PrincipalSid(sid)));
            } else {
                validateRepositoryId(session, repositoryType, repositoryId);
                getRepositoryAclService(repositoryType).addPermissions(repositoryId,
                    path,
                    buildPermissions(repositoryType, permissions),
                    List.of(new PrincipalSid(sid)));
            }
        } else {
            throw new NotFoundException("users.message", sid);
        }
    }

    @PutMapping(value = { "/{repositoryType:^design|prod$}/{repo-id}/group/{sid}",
            "/{repositoryType:^design|prod$}/group/{sid}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGroupPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        if (groupDao.getGroupByName(sid) != null) {
            if (StringUtils.isBlank(repositoryId)) {
                getRepositoryAclService(repositoryType).addRootPermissions(
                    buildPermissions(repositoryType, permissions),
                    List.of(new GrantedAuthoritySid(sid)));
            } else {
                validateRepositoryId(session, repositoryType, repositoryId);
                getRepositoryAclService(repositoryType).addPermissions(repositoryId,
                    path,
                    buildPermissions(repositoryType, permissions),
                    List.of(new GrantedAuthoritySid(sid)));
            }
        } else {
            throw new NotFoundException("group.message", sid);
        }
    }

    @DeleteMapping(value = { "/{repositoryType:^design|prod$}/{repo-id}", "/{repositoryType:^design|prod$}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) boolean force,
            HttpSession session) {
        SimpleRepositoryAclService repositoryAclService = getRepositoryAclService(repositoryType);
        if (StringUtils.isBlank(repositoryId)) {
            if (force) {
                repositoryAclService.deleteAclRoot();
            } else {
                repositoryAclService.removeRootPermissions();
            }
        } else {
            validateRepositoryId(session, repositoryType, repositoryId);
            if (force) {
                repositoryAclService.deleteAcl(repositoryId, path);
            } else {
                repositoryAclService.removePermissions(repositoryId, path);
            }
        }
    }

    @DeleteMapping(value = { "/{repositoryType:^design|prod$}/{repo-id}/user/{sid}",
            "/{repositoryType:^design|prod$}/user/{sid}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        if (StringUtils.isBlank(repositoryId)) {
            getRepositoryAclService(repositoryType).removeRootPermissions(buildPermissions(repositoryType, permissions),
                List.of(new PrincipalSid(sid)));
        } else {
            validateRepositoryId(session, repositoryType, repositoryId);
            getRepositoryAclService(repositoryType).removePermissions(repositoryId,
                path,
                buildPermissions(repositoryType, permissions),
                List.of(new PrincipalSid(sid)));
        }
    }

    @DeleteMapping(value = { "/{repositoryType:^design|prod$}/{repo-id}/group/{sid}",
            "/{repositoryType:^design|prod$}/group/{sid}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroupPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        if (StringUtils.isBlank(repositoryId)) {
            getRepositoryAclService(repositoryType).removeRootPermissions(buildPermissions(repositoryType, permissions),
                List.of(new GrantedAuthoritySid(sid)));
        } else {
            validateRepositoryId(session, repositoryType, repositoryId);
            getRepositoryAclService(repositoryType).removePermissions(repositoryId,
                path,
                buildPermissions(repositoryType, permissions),
                List.of(new GrantedAuthoritySid(sid)));
        }
    }

    private static class OwnerDetails {
        private final boolean principal;
        private final String sid;

        private OwnerDetails(String sid, boolean principal) {
            this.sid = sid;
            this.principal = principal;
        }

        public String getSid() {
            return sid;
        }

        public boolean isPrincipal() {
            return principal;
        }
    }

    @GetMapping(value = "/{repositoryType:^design|prod$}/{repo-id}/owner")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public OwnerDetails getOwner(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        Sid sid = getRepositoryAclService(repositoryType).getOwner(repositoryId, path);
        OwnerDetails ownerDetails;
        if (sid instanceof PrincipalSid) {
            PrincipalSid principalSid = (PrincipalSid) sid;
            ownerDetails = new OwnerDetails(principalSid.getPrincipal(), true);
        } else if (sid instanceof GrantedAuthoritySid) {
            GrantedAuthoritySid grantedAuthoritySid = (GrantedAuthoritySid) sid;
            ownerDetails = new OwnerDetails(grantedAuthoritySid.getGrantedAuthority(), true);
        } else {
            throw new IllegalStateException("Unsupported sid type.");
        }
        return ownerDetails;
    }

    @PutMapping(value = "/{repositoryType:^design|prod$}/{repo-id}/owner/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOwnerToUser(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        if (userDao.getUserByName(sid) != null) {
            getRepositoryAclService(repositoryType).updateOwner(repositoryId, path, new PrincipalSid(sid));
        } else {
            throw new NotFoundException("group.message", sid);
        }
    }

    @PutMapping(value = "/{repositoryType:^design|prod$}/{repo-id}/owner/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOwnerToGroup(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        if (groupDao.getGroupByName(sid) != null) {
            getRepositoryAclService(repositoryType).updateOwner(repositoryId, path, new GrantedAuthoritySid(sid));
        } else {
            throw new NotFoundException("group.message", sid);
        }
    }
}
