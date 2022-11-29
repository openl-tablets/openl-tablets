package org.openl.rules.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

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
@RequestMapping("/acl")
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
        List<Permission> permissionsList = Arrays.stream(permissions)
            .map(AclPermission::getPermission)
            .collect(Collectors.toList());
        if (REPO_TYPE_PROD.equals(repositoryType) && permissionsList.stream()
            .anyMatch(e -> !AclPermission.EDIT.equals(e))) {
            throw new NotFoundException("project.artifact.permission.message");
        }
        if (permissionsList.stream().anyMatch(Objects::isNull)) {
            throw new NotFoundException("project.artifact.permission.message");
        }
        return permissionsList;
    }

    private SimpleRepositoryAclService getRepositoryAclService(String repositoryType) {
        if (REPO_TYPE_PROD.equals(repositoryType)) {
            return productionRepositoryAclService;
        } else if (REPO_TYPE_DESIGN.equals(repositoryType)) {
            return repositoryAclService;
        }
        throw new NotFoundException("repository.type.message");
    }

    @GetMapping(value = "/{repositoryType}/repos")
    public Map<String, Map<String, String[]>> list(@PathVariable("repositoryType") String repositoryType) {
        Map<Sid, List<Permission>> permissions = getRepositoryAclService(repositoryType).listRootPermissions();
        return convert(permissions);
    }

    @GetMapping(value = "/{repositoryType}/repo/{repo-id}")
    public Map<String, Map<String, String[]>> list(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path) {
        Map<Sid, List<Permission>> permissions = getRepositoryAclService(repositoryType).listPermissions(repositoryId,
            path);
        return convert(permissions);
    }

    @PutMapping(value = "/{repositoryType}/repos/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addUserRootPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions) {
        List<Permission> permissionsList = buildPermissions(repositoryType, permissions);
        getRepositoryAclService(repositoryType).addRootPermissions(permissionsList, List.of(new PrincipalSid(sid)));
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
            throw new NotFoundException("repository.type.message");
        }
    }

    @PutMapping(value = "/{repositoryType}/repo/{repo-id}/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addUserPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        if (userDao.existsByName(sid)) {
            List<Permission> permissionsList = buildPermissions(repositoryType, permissions);
            getRepositoryAclService(repositoryType)
                .addPermissions(repositoryId, path, permissionsList, List.of(new PrincipalSid(sid)));
        } else {
            throw new NotFoundException("users.message", sid);
        }
    }

    @PutMapping(value = "/{repositoryType}/repos/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGroupRootPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions) {
        List<Permission> permissionsList = buildPermissions(repositoryType, permissions);
        getRepositoryAclService(repositoryType).addRootPermissions(permissionsList,
            List.of(new GrantedAuthoritySid(sid)));
    }

    @PutMapping(value = "/{repositoryType}/repo/{repo-id}/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGroupPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        if (groupDao.getGroupByName(sid) != null) {
            List<Permission> permissionsList = buildPermissions(repositoryType, permissions);
            getRepositoryAclService(repositoryType)
                .addPermissions(repositoryId, path, permissionsList, List.of(new GrantedAuthoritySid(sid)));
        } else {
            throw new NotFoundException("group.message", sid);
        }
    }

    @DeleteMapping(value = "/{repositoryType}/repos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllRootPermissions(@PathVariable("repositoryType") String repositoryType,
            @RequestParam(required = false) boolean recursive) {
        if (recursive) {
            getRepositoryAclService(repositoryType).deleteAclRoot();
        } else {
            getRepositoryAclService(repositoryType).removeRootPermissions();
        }
    }

    @DeleteMapping(value = "/{repositoryType}/repo/{repo-id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) boolean recursive,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        if (recursive) {
            getRepositoryAclService(repositoryType).deleteAcl(repositoryId, path);
        } else {
            getRepositoryAclService(repositoryType).removePermissions(repositoryId, path);
        }
    }

    @DeleteMapping(value = "/{repositoryType}/repos/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserRootPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions) {
        List<Permission> permissionsList = buildPermissions(repositoryType, permissions);
        getRepositoryAclService(repositoryType).removeRootPermissions(permissionsList, List.of(new PrincipalSid(sid)));
    }

    @DeleteMapping(value = "/{repositoryType}/repo/{repo-id}/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        List<Permission> permissionsList = buildPermissions(repositoryType, permissions);
        getRepositoryAclService(repositoryType)
            .removePermissions(repositoryId, path, permissionsList, List.of(new PrincipalSid(sid)));
    }

    @DeleteMapping(value = "/{repositoryType}/repos/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroupRootPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions) {
        List<Permission> permissionsList = buildPermissions(repositoryType, permissions);
        getRepositoryAclService(repositoryType).removeRootPermissions(permissionsList,
            List.of(new GrantedAuthoritySid(sid)));
    }

    @DeleteMapping(value = "/{repositoryType}/repo/{repo-id}/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroupPermissions(@PathVariable("repositoryType") String repositoryType,
            @PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryType, repositoryId);
        List<Permission> permissionsList = buildPermissions(repositoryType, permissions);
        getRepositoryAclService(repositoryType)
            .removePermissions(repositoryId, path, permissionsList, List.of(new GrantedAuthoritySid(sid)));
    }
}
