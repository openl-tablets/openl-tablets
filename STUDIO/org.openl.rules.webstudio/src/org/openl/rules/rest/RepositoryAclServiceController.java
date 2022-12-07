package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.Group;
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
    private static final String REPO_TYPE_DEPLOY_CONFIG = "deployConfig";
    private static final String REPO_TYPE_DESIGN = "design";

    private final RepositoryAclService designRepositoryAclService;
    private final RepositoryAclService deployConfigRepositoryAclService;
    private final SimpleRepositoryAclService productionRepositoryAclService;
    private final DeploymentManager deploymentManager;

    private final UserDao userDao;
    private final GroupDao groupDao;

    public RepositoryAclServiceController(UserDao userDao,
            GroupDao groupDao,
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
            @Qualifier("deployConfigRepositoryAclService") RepositoryAclService deployConfigRepositoryAclService,
            @Qualifier("productionRepositoryAclService") SimpleRepositoryAclService productionRepositoryAclService,
            DeploymentManager deploymentManager) {
        this.designRepositoryAclService = designRepositoryAclService;
        this.deployConfigRepositoryAclService = deployConfigRepositoryAclService;
        this.userDao = userDao;
        this.groupDao = groupDao;
        this.productionRepositoryAclService = productionRepositoryAclService;
        this.deploymentManager = deploymentManager;
    }

    public static class SidPermissionsDto {
        private Long groupId;
        private String username;
        private String groupName;
        private String[] permissions;

        public SidPermissionsDto() {
        }

        public SidPermissionsDto(Long groupId, String groupName, String[] permissions) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.permissions = permissions;
        }

        public SidPermissionsDto(String username, String[] permissions) {
            this.username = username;
            this.permissions = permissions;
        }

        public Long getGroupId() {
            return groupId;
        }

        public String getUsername() {
            return username;
        }

        public String getGroupName() {
            return groupName;
        }

        public String[] getPermissions() {
            return permissions;
        }
    }

    private List<SidPermissionsDto> convert(Map<Sid, List<Permission>> permissions) {
        List<SidPermissionsDto> ret = new ArrayList<>();
        for (Map.Entry<Sid, List<Permission>> entry : permissions.entrySet()) {
            String[] permissionsArray = entry.getValue().stream().map(e -> {
                AclPermission projectArtifactPermission = AclPermission.getPermission(e.getMask());
                if (projectArtifactPermission != null) {
                    return AclPermission.toString(projectArtifactPermission);
                } else {
                    return String.valueOf(e.getMask());
                }
            }).toArray(String[]::new);

            if (entry.getKey() instanceof PrincipalSid) {
                PrincipalSid principalSid = (PrincipalSid) entry.getKey();
                ret.add(new SidPermissionsDto(principalSid.getPrincipal(), permissionsArray));
            } else if (entry.getKey() instanceof GrantedAuthoritySid) {
                GrantedAuthoritySid grantedAuthoritySid = (GrantedAuthoritySid) entry.getKey();
                ret.add(
                    new SidPermissionsDto(groupDao.getGroupByName(grantedAuthoritySid.getGrantedAuthority()).getId(),
                        grantedAuthoritySid.getGrantedAuthority(),
                        permissionsArray));
            } else {
                throw new IllegalStateException("Unsupported sid type.");
            }
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
            permissionsList.add(aclPermission);
        }
        return permissionsList;
    }

    private SimpleRepositoryAclService getRepositoryAclService(String repositoryType) {
        if (REPO_TYPE_PROD.equals(repositoryType)) {
            return productionRepositoryAclService;
        } else if (REPO_TYPE_DESIGN.equals(repositoryType)) {
            return designRepositoryAclService;
        } else if (REPO_TYPE_DEPLOY_CONFIG.equals(repositoryType)) {
            return deployConfigRepositoryAclService;
        }
        throw new NotFoundException("repository.type.message", repositoryType);
    }

    private void validateRepositoryId(HttpSession session, String repositoryType, String repositoryId) {
        if (REPO_TYPE_DEPLOY_CONFIG.equals(repositoryType)) {
            UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);
            if (!Objects.equals(userWorkspace.getDesignTimeRepository().getDeployConfigRepository().getId(),
                repositoryId)) {
                throw new NotFoundException("repository.message", repositoryId);
            }
        } else if (REPO_TYPE_DESIGN.equals(repositoryType)) {
            UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);
            if (userWorkspace.getDesignTimeRepository()
                .getRepositories()
                .stream()
                .noneMatch(e -> Objects.equals(e.getId(), repositoryId))) {
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

    @GetMapping(value = { "/{repositoryType:^design|prod|deployConfig$}/{repo-id}",
            "{repositoryType:^design|prod|deployConfig$}" })
    public List<SidPermissionsDto> list(@PathVariable("repositoryType") String repositoryType,
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

    @PutMapping(value = { "/{repositoryType:^design|prod|deployConfig$}/{repo-id}/user/{username}",
            "/{repositoryType:^design|prod|deployConfig$}/user/{username}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addUserPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("username") String username,
            @RequestParam String[] permissions,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        if (userDao.existsByName(username)) {
            if (StringUtils.isBlank(repositoryId)) {
                getRepositoryAclService(repositoryType).addRootPermissions(
                    buildPermissions(repositoryType, permissions),
                    List.of(new PrincipalSid(username)));
            } else {
                validateRepositoryId(session, repositoryType, repositoryId);
                getRepositoryAclService(repositoryType).addPermissions(repositoryId,
                    path,
                    buildPermissions(repositoryType, permissions),
                    List.of(new PrincipalSid(username)));
            }
        } else {
            throw new NotFoundException("users.message", username);
        }
    }

    @PutMapping(value = { "/{repositoryType:^design|prod|deployConfig$}/{repo-id}/group/{id}",
            "/{repositoryType:^design|prod|deployConfig$}/group/{id}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGroupPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("id") Long id,
            @RequestParam String[] permissions,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        Group group = groupDao.getGroupById(id);
        if (group != null) {
            if (StringUtils.isBlank(repositoryId)) {
                getRepositoryAclService(repositoryType).addRootPermissions(
                    buildPermissions(repositoryType, permissions),
                    List.of(new GrantedAuthoritySid(group.getName())));
            } else {
                validateRepositoryId(session, repositoryType, repositoryId);
                getRepositoryAclService(repositoryType).addPermissions(repositoryId,
                    path,
                    buildPermissions(repositoryType, permissions),
                    List.of(new GrantedAuthoritySid(group.getName())));
            }
        } else {
            throw new NotFoundException("group.message");
        }
    }

    @DeleteMapping(value = { "/{repositoryType:^design|prod|deployConfig$}/{repo-id}",
            "/{repositoryType:^design|prod|deployConfig$}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        SimpleRepositoryAclService repositoryAclService = getRepositoryAclService(repositoryType);
        if (StringUtils.isBlank(repositoryId)) {
            repositoryAclService.removeRootPermissions();
        } else {
            validateRepositoryId(session, repositoryType, repositoryId);
            repositoryAclService.removePermissions(repositoryId, path);
        }
    }

    @DeleteMapping(value = { "/{repositoryType:^design|prod|deployConfig$}/{repo-id}/user/{username}",
            "/{repositoryType:^design|prod|deployConfig$}/user/{username}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("username") String username,
            @RequestParam String[] permissions,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        if (StringUtils.isBlank(repositoryId)) {
            getRepositoryAclService(repositoryType).removeRootPermissions(buildPermissions(repositoryType, permissions),
                List.of(new PrincipalSid(username)));
        } else {
            validateRepositoryId(session, repositoryType, repositoryId);
            getRepositoryAclService(repositoryType).removePermissions(repositoryId,
                path,
                buildPermissions(repositoryType, permissions),
                List.of(new PrincipalSid(username)));
        }
    }

    @DeleteMapping(value = { "/{repositoryType:^design|prod|deployConfig$}/{repo-id}/group/{id}",
            "/{repositoryType:^design|prod|deployConfig$}/group/{id}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroupPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("id") Long id,
            @RequestParam String[] permissions,
            @PathVariable(value = "repo-id", required = false) String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        Group group = groupDao.getGroupById(id);
        if (group != null) {
            if (StringUtils.isBlank(repositoryId)) {
                getRepositoryAclService(repositoryType).removeRootPermissions(
                    buildPermissions(repositoryType, permissions),
                    List.of(new GrantedAuthoritySid(group.getName())));
            } else {
                validateRepositoryId(session, repositoryType, repositoryId);
                getRepositoryAclService(repositoryType).removePermissions(repositoryId,
                    path,
                    buildPermissions(repositoryType, permissions),
                    List.of(new GrantedAuthoritySid(group.getName())));
            }
        } else {
            throw new NotFoundException("group.message");
        }
    }

    public static class SidDto {
        private Long groupId;
        private String username;
        private String groupName;

        public SidDto() {
        }

        public SidDto(Long groupId, String groupName) {
            this.groupId = groupId;
            this.groupName = groupName;
        }

        public SidDto(String username) {
            this.username = username;
        }

        public Long getGroupId() {
            return groupId;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }
    }

    @GetMapping(value = "/{repositoryType:^design|prod|deployConfig$}/{repo-id}/owner")
    public SidDto getOwner(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        Sid sid = getRepositoryAclService(repositoryType).getOwner(repositoryId, path);
        SidDto sidDto;
        if (sid instanceof PrincipalSid) {
            PrincipalSid principalSid = (PrincipalSid) sid;
            sidDto = new SidDto(principalSid.getPrincipal());
        } else if (sid instanceof GrantedAuthoritySid) {
            GrantedAuthoritySid grantedAuthoritySid = (GrantedAuthoritySid) sid;
            sidDto = new SidDto(groupDao.getGroupByName(grantedAuthoritySid.getGrantedAuthority()).getId(),
                grantedAuthoritySid.getGrantedAuthority());
        } else {
            throw new IllegalStateException("Unsupported sid type.");
        }
        return sidDto;
    }

    @PutMapping(value = "/{repositoryType:^design|prod|deployConfig$}/{repo-id}/owner/user/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOwnerToUser(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("username") String username,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        if (userDao.getUserByName(username) != null) {
            if (!getRepositoryAclService(repositoryType).updateOwner(repositoryId, path, new PrincipalSid(username))) {
                throw new ConflictException("owner.message");
            }
        } else {
            throw new NotFoundException("group.message");
        }
    }

    @PutMapping(value = "/{repositoryType:^design|prod|deployConfig$}/{repo-id}/owner/group/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOwnerToGroup(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("id") Long id,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        Group group = groupDao.getGroupById(id);
        if (group != null) {
            if (!getRepositoryAclService(repositoryType)
                .updateOwner(repositoryId, path, new GrantedAuthoritySid(group.getName()))) {
                throw new ConflictException("owner.message");
            }
        } else {
            throw new NotFoundException("group.message");
        }
    }
}
