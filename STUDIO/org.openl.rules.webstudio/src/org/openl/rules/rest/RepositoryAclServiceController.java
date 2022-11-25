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
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
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

    private final RepositoryAclService repositoryAclService;

    private final UserDao userDao;
    private final GroupDao groupDao;

    public RepositoryAclServiceController(UserDao userDao,
            GroupDao groupDao,
            RepositoryAclService repositoryAclService) {
        this.repositoryAclService = repositoryAclService;
        this.userDao = userDao;
        this.groupDao = groupDao;
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

    private static List<Permission> buildPermissions(String[] permissions) {
        if (permissions == null) {
            return Collections.emptyList();
        }
        List<Permission> permissionsList = Arrays.stream(permissions)
            .map(AclPermission::getPermission)
            .collect(Collectors.toList());
        if (permissionsList.stream().anyMatch(Objects::isNull)) {
            throw new NotFoundException("project.artifact.permission.message");
        }
        return permissionsList;
    }

    @GetMapping(value = "/repos")
    public Map<String, Map<String, String[]>> list() {
        Map<Sid, List<Permission>> permissions = repositoryAclService.listRootPermissions();
        return convert(permissions);
    }

    @GetMapping(value = "/repo/{repo-id}")
    public Map<String, Map<String, String[]>> list(@PathVariable("repo-id") String repositoryId,
            @RequestParam String path) {
        Map<Sid, List<Permission>> permissions = repositoryAclService.listPermissions(repositoryId, path);
        return convert(permissions);
    }

    @PutMapping(value = "/repos/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addUserRootPermissions(@PathVariable("sid") String sid, @RequestParam String[] permissions) {
        List<Permission> permissionsList = buildPermissions(permissions);
        repositoryAclService.addRootPermissions(permissionsList, List.of(new PrincipalSid(sid)));
    }

    private void validateRepositoryId(HttpSession session, String repositoryId) {
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);
        if (userWorkspace.getDesignTimeRepository()
            .getRepositories()
            .stream()
            .noneMatch(e -> Objects.equals(e.getId(), repositoryId)) && !Objects
                .equals(userWorkspace.getDesignTimeRepository().getDeployConfigRepository().getId(), repositoryId)) {
            throw new NotFoundException("repository.message", repositoryId);
        }
    }

    @PutMapping(value = "/repo/{repo-id}/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addUserPermissions(@PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryId);
        if (userDao.existsByName(sid)) {
            List<Permission> permissionsList = buildPermissions(permissions);
            repositoryAclService.addPermissions(repositoryId, path, permissionsList, List.of(new PrincipalSid(sid)));
        } else {
            throw new NotFoundException("users.message", sid);
        }
    }

    @PutMapping(value = "/repos/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGroupRootPermissions(@PathVariable("sid") String sid, @RequestParam String[] permissions) {
        List<Permission> permissionsList = buildPermissions(permissions);
        repositoryAclService.addRootPermissions(permissionsList, List.of(new GrantedAuthoritySid(sid)));
    }

    @PutMapping(value = "/repo/{repo-id}/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGroupPermissions(@PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryId);
        if (groupDao.getGroupByName(sid) != null) {
            List<Permission> permissionsList = buildPermissions(permissions);
            repositoryAclService
                .addPermissions(repositoryId, path, permissionsList, List.of(new GrantedAuthoritySid(sid)));
        } else {
            throw new NotFoundException("group.message", sid);
        }
    }

    @DeleteMapping(value = "/repos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllRootPermissions(@RequestParam(required = false) boolean recursive) {
        if (recursive) {
            repositoryAclService.deleteAclRoot();
        } else {
            repositoryAclService.removeRootPermissions();
        }
    }

    @DeleteMapping(value = "/repo/{repo-id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllPermissions(@PathVariable("repo-id") String repositoryId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) boolean recursive,
            HttpSession session) {
        validateRepositoryId(session, repositoryId);
        if (recursive) {
            repositoryAclService.deleteAcl(repositoryId, path);
        } else {
            repositoryAclService.removePermissions(repositoryId, path);
        }
    }

    @DeleteMapping(value = "/repos/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserRootPermissions(@PathVariable("sid") String sid, @RequestParam String[] permissions) {
        List<Permission> permissionsList = buildPermissions(permissions);
        repositoryAclService.removeRootPermissions(permissionsList, List.of(new PrincipalSid(sid)));
    }

    @DeleteMapping(value = "/repo/{repo-id}/user/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserPermissions(@PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryId);
        List<Permission> permissionsList = buildPermissions(permissions);
        repositoryAclService.removePermissions(repositoryId, path, permissionsList, List.of(new PrincipalSid(sid)));
    }

    @DeleteMapping(value = "/repos/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroupRootPermissions(@PathVariable("sid") String sid, @RequestParam String[] permissions) {
        List<Permission> permissionsList = buildPermissions(permissions);
        repositoryAclService.removeRootPermissions(permissionsList, List.of(new GrantedAuthoritySid(sid)));
    }

    @DeleteMapping(value = "/repo/{repo-id}/group/{sid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroupPermissions(@PathVariable("sid") String sid,
            @RequestParam String[] permissions,
            @PathVariable("repo-id") String repositoryId,
            @RequestParam String path,
            HttpSession session) {
        validateRepositoryId(session, repositoryId);
        List<Permission> permissionsList = buildPermissions(permissions);
        repositoryAclService
            .removePermissions(repositoryId, path, permissionsList, List.of(new GrantedAuthoritySid(sid)));
    }
}
