package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.RepositoryException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/acl/repo")
@Tag(name = "ACL Management")
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
        Collection<AclPermission> supportedPermissions;
        if (REPO_TYPE_DESIGN.equalsIgnoreCase(repositoryType)) {
            supportedPermissions = AclPermission.ALL_SUPPORTED_DESIGN_REPO_PERMISSIONS;
        } else if (REPO_TYPE_PROD.equalsIgnoreCase(repositoryType)) {
            supportedPermissions = AclPermission.ALL_SUPPORTED_PROD_REPO_PERMISSIONS;
        } else if (REPO_TYPE_DEPLOY_CONFIG.equalsIgnoreCase(repositoryType)) {
            supportedPermissions = AclPermission.ALL_SUPPORTED_DEPLOY_CONFIG_REPO_PERMISSIONS;
        } else {
            throw new IllegalStateException("Unknown repository type: " + repositoryType);
        }
        for (String permission : permissions) {
            AclPermission aclPermission = AclPermission.getPermission(permission);
            if (aclPermission == null) {
                throw new NotFoundException("repository.permission.message", permission);
            }
            if (!supportedPermissions.contains(aclPermission)) {
                throw new BadRequestException(String.format("Permission %s is not supported for repository type '%s'.", AclPermission.toString(aclPermission), repositoryType));
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
            if (!userWorkspace.getDesignTimeRepository().hasDeployConfigRepo() || !Objects
                .equals(userWorkspace.getDesignTimeRepository().getDeployConfigRepository().getId(), repositoryId)) {
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

    @GetMapping(value = { "/artifacts/{repositoryType}/{repo-id}" })
    @Operation(summary = "mgmt.acl.list-artifacts.summary", description = "mgmt.acl.list-artifacts.desc")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public List<String> listArtifacts(
            @Parameter(description = "repo.acl.param.repository-type.desc") @PathVariable("repositoryType") String repositoryType,
            @Parameter(description = "repo.acl.param.repository-id.desc") @PathVariable(value = "repo-id") String repositoryId,
            HttpSession session) {
        if (REPO_TYPE_DEPLOY_CONFIG.equals(repositoryType)) {
            UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);
            if (!userWorkspace.getDesignTimeRepository().hasDeployConfigRepo() || !Objects
                .equals(userWorkspace.getDesignTimeRepository().getDeployConfigRepository().getId(), repositoryId)) {
                throw new NotFoundException("repository.message", repositoryId);
            }
            try {
                return userWorkspace.getDesignTimeRepository()
                    .getDDProjects()
                    .stream()
                    .map(e -> e.getFileData().getName())
                    .collect(Collectors.toList());
            } catch (RepositoryException ignored) {
            }
        } else if (REPO_TYPE_DESIGN.equals(repositoryType)) {
            UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);
            if (userWorkspace.getDesignTimeRepository()
                .getRepositories()
                .stream()
                .noneMatch(e -> Objects.equals(e.getId(), repositoryId))) {
                throw new NotFoundException("repository.message", repositoryId);
            }
            return userWorkspace.getDesignTimeRepository()
                .getProjects()
                .stream()
                .filter(e -> Objects.equals(e.getRepository().getId(), repositoryId))
                .map(e -> e.getFileData().getName())
                .collect(Collectors.toList());
        }
        throw new BadRequestException("Invalid repository");
    }

    private static String toRepoTypeString(AclCommandSupport.RepoType repoType) {
        if (AclCommandSupport.RepoType.PROD == repoType) {
            return REPO_TYPE_PROD;
        } else if (AclCommandSupport.RepoType.DESIGN == repoType) {
            return REPO_TYPE_DESIGN;
        } else if (AclCommandSupport.RepoType.DEPLOY_CONFIG == repoType) {
            return REPO_TYPE_DEPLOY_CONFIG;
        }
        throw new IllegalStateException("Unsupported repository type");
    }

    private static String[] listAllSupportedPermissions(AclCommandSupport.RepoType repoType) {
        return AclCommandSupport.listAllSupportedPermissions(repoType)
            .stream()
            .map(e -> AclPermission.toString(e))
            .toArray(String[]::new);
    }

    @Transactional
    @PostMapping(value = { "/runScript" }, consumes = { MediaType.TEXT_PLAIN })
    @Operation(summary = "mgmt.acl.run-script.summary", description = "mgmt.acl.run-script.desc")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.TEXT_PLAIN))
    public String runScript(
            @Parameter(description = "repo.acl.param.content.desc", content = @Content(examples = @ExampleObject("-- this line prevents system from accidental changes\n# List all permissions for all design repositories\nlist:design:\n\n# List all permissions for Example 1 - Bank Rating in 'design' repo\nlistAll:design/design:DESIGN/rules/Example 1 - Bank Rating\n\n# Add VIEW, EDIT, and DELETE permissions to Example 1 - Bank Rating for the user with username 'user'\nadd:design/design:DESIGN/rules/Example 1 - Bank Rating:user:user:VIEW,EDIT,DELETE\n\n# Set VIEW permission to Example 1 - Bank Rating for group with name 'Viewers'\nset:design::group:Viewers:VIEW\n\n# Remove VIEW permission from Example 1 - Bank Rating for group with name 'Viewers'\nremove:design::group:Viewers:VIEW"))) @RequestBody String content,
            HttpSession session) {
        StringBuilder ret = new StringBuilder();
        try (Scanner scanner = new Scanner(content)) {
            int lineNum = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNum++;
                if (line.contains("#")) {
                    line = line.substring(0, line.indexOf("#"));
                }
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                try {
                    AclCommandSupport.AclCommand command = AclCommandSupport.toCommand(line);
                    if (AclCommandSupport.Action.ADD == command.action || AclCommandSupport.Action.SET == command.action) {
                        if (AclCommandSupport.SidType.USERNAME == command.sidType) {
                            if (AclCommandSupport.Action.SET == command.action) {
                                deleteUserPermissions(toRepoTypeString(command.repoType),
                                    command.sid,
                                    listAllSupportedPermissions(command.repoType),
                                    command.repo,
                                    command.resource,
                                    session);
                            }
                            addUserPermissions(toRepoTypeString(command.repoType),
                                command.sid,
                                command.permissions,
                                command.repo,
                                command.resource,
                                session);
                        } else if (AclCommandSupport.SidType.GROUP_NAME == command.sidType) {
                            Group group = groupDao.getGroupByName(command.sid);
                            if (group == null) {
                                throw new NotFoundException("group.message");
                            }
                            if (AclCommandSupport.Action.SET == command.action) {
                                deleteGroupPermissions(toRepoTypeString(command.repoType),
                                    group.getId(),
                                    listAllSupportedPermissions(command.repoType),
                                    command.repo,
                                    command.resource,
                                    session);
                            }
                            addGroupPermissions(toRepoTypeString(command.repoType),
                                group.getId(),
                                command.permissions,
                                command.repo,
                                command.resource,
                                session);
                        } else if (AclCommandSupport.SidType.GROUP_ID == command.sidType) {
                            if (AclCommandSupport.Action.SET == command.action) {
                                deleteGroupPermissions(toRepoTypeString(command.repoType),
                                    Long.valueOf(command.sid),
                                    listAllSupportedPermissions(command.repoType),
                                    command.repo,
                                    command.resource,
                                    session);
                            }
                            addGroupPermissions(toRepoTypeString(command.repoType),
                                Long.valueOf(command.sid),
                                command.permissions,
                                command.repo,
                                command.resource,
                                session);
                        }
                    } else if (AclCommandSupport.Action.REMOVE == command.action) {
                        if (AclCommandSupport.SidType.USERNAME == command.sidType) {
                            deleteUserPermissions(toRepoTypeString(command.repoType),
                                command.sid,
                                command.permissions,
                                command.repo,
                                command.resource,
                                session);
                        } else if (AclCommandSupport.SidType.GROUP_NAME == command.sidType) {
                            Group group = groupDao.getGroupByName(command.sid);
                            if (group == null) {
                                throw new NotFoundException("group.message");
                            }
                            deleteGroupPermissions(toRepoTypeString(command.repoType),
                                group.getId(),
                                command.permissions,
                                command.repo,
                                command.resource,
                                session);
                        } else if (AclCommandSupport.SidType.GROUP_ID == command.sidType) {
                            deleteGroupPermissions(toRepoTypeString(command.repoType),
                                Long.valueOf(command.sid),
                                command.permissions,
                                command.repo,
                                command.resource,
                                session);
                        }
                    } else if (AclCommandSupport.Action.LIST == command.action || AclCommandSupport.Action.LIST_ALL == command.action) {
                        if (ret.length() > 0 && ret.charAt(ret.length() - 1) != '\n') {
                            ret.append("\n");
                        }
                        if (ret.length() > 0) {
                            ret.append("\n");
                        }
                        ret.append(AclCommandSupport.Action.LIST == command.action ? "list:" : "listAll:")
                            .append(toResourceLocationString(command.repoType, command.repo, command.resource))
                            .append("\n");
                        StringBuilder sb = new StringBuilder();

                        String repo = command.repo;
                        String resource = command.resource;
                        while (resource.endsWith("/")) {
                            resource = resource.substring(0, resource.length() - 1);
                        }
                        if (StringUtils.isBlank(resource)) {
                            resource = null;
                        }
                        boolean f = false;
                        while (!f) {
                            if (repo == null || AclCommandSupport.Action.LIST_ALL != command.action) {
                                f = true;
                            }
                            List<SidPermissionsDto> permissions = list(toRepoTypeString(command.repoType),
                                repo,
                                resource,
                                session);
                            for (SidPermissionsDto permission : permissions) {
                                if (sb.length() > 0) {
                                    sb.append("\n");
                                }
                                sb.append("    ");
                                sb.append(toResourceLocationString(command.repoType, repo, resource));
                                sb.append(":")
                                    .append(permission.username != null ? "user:" + permission.username
                                                                        : "group:" + permission.groupName);
                                sb.append(":");
                                sb.append(String.join(",", permission.permissions));
                            }
                            if (resource != null) {
                                if (StringUtils.isBlank(resource) || resource.trim().equals("/")) {
                                    resource = null;
                                } else {
                                    int x = resource.lastIndexOf("/");
                                    if (x > 0) {
                                        resource = resource.substring(0, x);
                                    } else {
                                        resource = null;
                                    }
                                }
                            } else {
                                repo = null;
                            }
                        }
                        ret.append(sb);
                    }
                } catch (NotFoundException e) {
                    throw e;
                } catch (Exception e) {
                    throw new BadRequestException(String.format("Bad command at line %s: ", lineNum) + e.getMessage());
                }
            }
        }
        return ret.toString();
    }

    private static String toResourceLocationString(AclCommandSupport.RepoType repoType, String repo, String resource) {
        StringBuilder sb = new StringBuilder();
        sb.append(toRepoTypeString(repoType));
        if (!StringUtils.isBlank(repo)) {
            sb.append("/").append(repo);
        }
        sb.append(":");
        if (!StringUtils.isBlank(resource)) {
            if (!resource.startsWith("/")) {
                sb.append("/");
            }
            sb.append(resource);
        }
        return sb.toString();
    }

    @GetMapping(value = { "/{repositoryType:^design|prod|deployConfig$}/{repo-id}",
            "{repositoryType:^design|prod|deployConfig$}" })
    @Hidden
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
    @Hidden
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
    @Hidden
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
    @Hidden
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
    @Hidden
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
    @Hidden
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
    @Hidden
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
    @Hidden
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
    @Hidden
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
