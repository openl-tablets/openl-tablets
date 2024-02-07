package org.openl.rules.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.config.InMemoryProperties;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.model.GroupSettingsModel;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.security.acl.JdbcMutableAclService;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.util.StreamUtils;
import org.openl.util.StringUtils;

/**
 * Manages Users and Groups.
 *
 * @author Yury Molchan
 */
@RestController
@RequestMapping(value = "/admin/management", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Management")
public class ManagementController {

    private static final String SECURITY_DEF_GROUP_PROP = "security.default-group";
    private static final Set<String> DATABASE_PRIVILEGES = Arrays.stream(Privileges.values())
            .map(Privileges::getName)
            .collect(Collectors.toSet());

    private final GroupDao groupDao;
    private final GroupManagementService groupManagementService;
    private final InMemoryProperties properties;
    private final BeanValidationProvider validationProvider;
    private final ExternalGroupService extGroupService;
    private final JdbcMutableAclService aclService;
    private final RepositoryAclService designRepositoryAclService;
    private final RepositoryAclService deployConfigRepositoryAclService;
    private final SimpleRepositoryAclService productionRepositoryAclService;

    @Autowired
    public ManagementController(GroupDao groupDao,
                                GroupManagementService groupManagementService,
                                InMemoryProperties properties,
                                BeanValidationProvider validationProvider,
                                ExternalGroupService extGroupService,
                                @Autowired(required = false) JdbcMutableAclService aclService,
                                @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
                                @Qualifier("deployConfigRepositoryAclService") RepositoryAclService deployConfigRepositoryAclService,
                                @Qualifier("productionRepositoryAclService") SimpleRepositoryAclService productionRepositoryAclService) {
        this.groupDao = groupDao;
        this.groupManagementService = groupManagementService;
        this.properties = properties;
        this.validationProvider = validationProvider;
        this.extGroupService = extGroupService;
        this.aclService = aclService;
        this.designRepositoryAclService = designRepositoryAclService;
        this.deployConfigRepositoryAclService = deployConfigRepositoryAclService;
        this.productionRepositoryAclService = productionRepositoryAclService;
    }

    @Operation(description = "mgmt.get-groups.desc", summary = "mgmt.get-groups.summary")
    @GetMapping("/groups")
    public Map<String, UIGroup> getGroups() {
        SecurityChecker.allow(Privileges.ADMIN);
        return groupDao.getAllGroups().stream().collect(StreamUtils.toLinkedMap(Group::getName, UIGroup::new));
    }

    private Set<String> getGroupUiPrivileges(Supplier<Map<Sid, List<Permission>>> g, Function<Permission, String> f) {
        Map<Sid, List<Permission>> groupPermissions = g.get();
        Set<String> uiPrivileges = new HashSet<>();
        if (!groupPermissions.isEmpty()) {
            List<Permission> permissions = groupPermissions.values().iterator().next();
            for (Permission permission : permissions) {
                String designPrivilege = f.apply(permission);
                if (designPrivilege != null) {
                    uiPrivileges.add(designPrivilege);
                }
            }
        }
        return uiPrivileges;
    }

    private UIGroup buildUIGroup(Group group) {
        UIGroup uiGroup = new UIGroup(group);
        List<Sid> grantedAuthoritySidList = Collections.singletonList(new GrantedAuthoritySid(group.getName()));
        uiGroup.privileges
                .addAll(getGroupUiPrivileges(() -> designRepositoryAclService.listRootPermissions(grantedAuthoritySidList),
                        DESIGN_PRIVILEGES::get));
        uiGroup.privileges.addAll(
                getGroupUiPrivileges(() -> deployConfigRepositoryAclService.listRootPermissions(grantedAuthoritySidList),
                        DEPLOY_CONFIG_PRIVILEGES::get));
        return uiGroup;
    }

    @Operation(description = "mgmt.get-groups.desc", summary = "mgmt.get-groups.summary", hidden = true)
    @GetMapping("/old/groups")
    @Deprecated
    public Map<String, UIGroup> getOldGroups() {
        SecurityChecker.allow(Privileges.ADMIN);
        return groupDao.getAllGroups().stream().collect(StreamUtils.toLinkedMap(Group::getName, this::buildUIGroup));
    }

    @Operation(description = "mgmt.delete-group.desc", summary = "mgmt.delete-group.summary")
    @DeleteMapping(value = "/groups/{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteGroup(@Parameter(description = "mgmt.schema.group.id") @PathVariable("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        Group group = groupDao.getGroupById(id);
        groupDao.deleteGroupById(id);
        if (group != null && aclService != null) {
            aclService.deleteSid(new GrantedAuthoritySid(group.getName()));
        }
    }

    @Operation(description = "mgmt.save-group.desc", summary = "mgmt.save-group.summary")
    @PostMapping(value = "/groups", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void saveGroup(
            @Parameter(description = "mgmt.schema.group.old-name") @RequestParam(value = "oldName", required = false) final String oldName,
            @Parameter(description = "mgmt.schema.group.name", required = true) @RequestParam("name") final String name,
            @Parameter(description = "mgmt.schema.group.description") @RequestParam(value = "description", required = false) final String description,
            @Parameter(description = "mgmt.schema.group.sub-groups") @RequestParam(value = "group", required = false) final Set<String> roles,
            @Parameter(description = "mgmt.schema.group.privileges") @RequestParam(value = "privilege", required = false) final Set<String> privileges) {
        SecurityChecker.allow(Privileges.ADMIN);
        if (!name.equals(oldName) && groupManagementService.isGroupExist(name)) {
            throw new ConflictException("duplicated.group.message");
        }
        if (StringUtils.isBlank(oldName)) {
            groupManagementService.addGroup(name, description);
        } else {
            groupManagementService.updateGroup(oldName, name, description);
        }

        groupManagementService.updateGroup(name,
                roles,
                privileges == null ? null
                        : privileges.stream().filter(DATABASE_PRIVILEGES::contains).collect(Collectors.toSet()));
        GrantedAuthoritySid grantedAuthoritySid = new GrantedAuthoritySid(name);
        designRepositoryAclService.removeRootPermissions(Collections.singletonList(grantedAuthoritySid));
        deployConfigRepositoryAclService.removeRootPermissions(Collections.singletonList(grantedAuthoritySid));
        productionRepositoryAclService.removeRootPermissions(List.of(AclPermission.EDIT),
                Collections.singletonList(grantedAuthoritySid));
        if (privileges != null) {
            List<Permission> designPermissions = toPermissions(privileges, DESIGN_PRIVILEGES::getKey);
            designRepositoryAclService.addRootPermissions(designPermissions,
                    Collections.singletonList(grantedAuthoritySid));

            List<Permission> deployConfigPermissions = toPermissions(privileges, DEPLOY_CONFIG_PRIVILEGES::getKey);
            deployConfigRepositoryAclService.addRootPermissions(deployConfigPermissions,
                    Collections.singletonList(grantedAuthoritySid));

            List<Permission> productionPermissions = toPermissions(privileges, PRODUCTION_PRIVILEGES::getKey);
            if (!productionPermissions.isEmpty()) {
                productionRepositoryAclService.addRootPermissions(
                        List.of(AclPermission.VIEW, AclPermission.EDIT, AclPermission.DELETE),
                        Collections.singletonList(grantedAuthoritySid));
            }

        }
    }

    private static List<Permission> toPermissions(Set<String> privileges, Function<String, Permission> mapper) {
        return privileges.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Operation(description = "mgmt.save-settings.desc", summary = "mgmt.save-settings.summary")
    @PostMapping(value = "/groups/settings", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void saveSettings(@RequestBody GroupSettingsModel request) {
        SecurityChecker.allow(Privileges.ADMIN);
        validationProvider.validate(request);
        properties.setProperty(SECURITY_DEF_GROUP_PROP, request.getDefaultGroup());
    }

    @Operation(description = "mgmt.get-settings.desc", summary = "mgmt.get-settings.summary")
    @GetMapping("/groups/settings")
    public GroupSettingsModel getSettings() {
        GroupSettingsModel model = new GroupSettingsModel();
        model.setDefaultGroup(properties.getProperty(SECURITY_DEF_GROUP_PROP));
        return model;
    }

    public static final Map<String, String> UI_PRIVILEGES;
    public static final BidiMap<Permission, String> DESIGN_PRIVILEGES;
    public static final BidiMap<Permission, String> DEPLOY_CONFIG_PRIVILEGES;
    public static final BidiMap<Permission, String> PRODUCTION_PRIVILEGES;

    static {
        BidiMap<Permission, String> designPrivileges = new DualLinkedHashBidiMap<>();
        designPrivileges.put(AclPermission.VIEW, "VIEW_PROJECTS");
        designPrivileges.put(AclPermission.CREATE, "CREATE_PROJECTS");
        designPrivileges.put(AclPermission.ADD, "ADD");
        designPrivileges.put(AclPermission.EDIT, "EDIT_PROJECTS");
        designPrivileges.put(AclPermission.DELETE, "DELETE_PROJECTS");
        designPrivileges.put(AclPermission.ERASE, "ERASE_PROJECTS");
        designPrivileges.put(AclPermission.RUN, "RUN");
        designPrivileges.put(AclPermission.BENCHMARK, "BENCHMARK");
        DESIGN_PRIVILEGES = UnmodifiableBidiMap.unmodifiableBidiMap(designPrivileges);

        BidiMap<Permission, String> deployConfigPrivileges = new DualLinkedHashBidiMap<>();
        deployConfigPrivileges.put(AclPermission.VIEW, "VIEW_DEPLOYMENT");
        deployConfigPrivileges.put(AclPermission.CREATE, "CREATE_DEPLOYMENT");
        deployConfigPrivileges.put(AclPermission.EDIT, "EDIT_DEPLOYMENT");
        deployConfigPrivileges.put(AclPermission.DELETE, "DELETE_DEPLOYMENT");
        deployConfigPrivileges.put(AclPermission.ERASE, "ERASE_DEPLOYMENT");
        deployConfigPrivileges.put(AclPermission.DEPLOY, "DEPLOY_PROJECTS");
        DEPLOY_CONFIG_PRIVILEGES = UnmodifiableBidiMap.unmodifiableBidiMap(deployConfigPrivileges);

        BidiMap<Permission, String> productionPrivileges = new DualLinkedHashBidiMap<>();
        productionPrivileges.put(AclPermission.VIEW, "DEPLOY_PROJECTS");
        productionPrivileges.put(AclPermission.EDIT, "DEPLOY_PROJECTS");
        productionPrivileges.put(AclPermission.DELETE, "DEPLOY_PROJECTS");
        PRODUCTION_PRIVILEGES = UnmodifiableBidiMap.unmodifiableBidiMap(productionPrivileges);

        Map<String, String> privileges = new LinkedHashMap<>();
        privileges.put(DESIGN_PRIVILEGES.get(AclPermission.VIEW), "View Projects");
        privileges.put(DESIGN_PRIVILEGES.get(AclPermission.CREATE), "Create Projects");
        privileges.put(DESIGN_PRIVILEGES.get(AclPermission.ADD), "Add Resource to Projects");
        privileges.put(DESIGN_PRIVILEGES.get(AclPermission.EDIT), "Edit Projects");
        privileges.put(DESIGN_PRIVILEGES.get(AclPermission.DELETE), "Delete Projects");
        privileges.put(DESIGN_PRIVILEGES.get(AclPermission.ERASE), "Erase Projects");
        privileges.put(DESIGN_PRIVILEGES.get(AclPermission.RUN), "Run and Trace Tables");
        privileges.put(DESIGN_PRIVILEGES.get(AclPermission.BENCHMARK), "Benchmark Tables");
        privileges.put(Privileges.UNLOCK_PROJECTS.getName(), Privileges.UNLOCK_PROJECTS.getDisplayName());

        privileges.put(DEPLOY_CONFIG_PRIVILEGES.get(AclPermission.VIEW), "View Deploy Configuration");
        privileges.put(DEPLOY_CONFIG_PRIVILEGES.get(AclPermission.CREATE), "Create Deploy Configuration");
        privileges.put(DEPLOY_CONFIG_PRIVILEGES.get(AclPermission.EDIT), "Edit Deploy Configuration");
        privileges.put(DEPLOY_CONFIG_PRIVILEGES.get(AclPermission.DELETE), "Delete Deploy Configuration");
        privileges.put(DEPLOY_CONFIG_PRIVILEGES.get(AclPermission.ERASE), "Erase Deploy Configuration");
        privileges.put(DEPLOY_CONFIG_PRIVILEGES.get(AclPermission.DEPLOY), "Deploy Projects");
        privileges.put(Privileges.UNLOCK_DEPLOYMENT.getName(), Privileges.UNLOCK_DEPLOYMENT.getDisplayName());

        privileges.put(Privileges.ADMIN.getName(), Privileges.ADMIN.getDisplayName());

        UI_PRIVILEGES = Collections.unmodifiableMap(privileges);
    }

    @Operation(description = "mgmt.get-privileges.desc", summary = "mgmt.get-privileges.summary")
    @GetMapping("/privileges")
    public Map<String, String> getPrivileges() {
        SecurityChecker.allow(Privileges.ADMIN);
        return UI_PRIVILEGES;
    }

    @Operation(description = "mgmt.search-external-groups.desc", summary = "mgmt.search-external-groups.summary")
    @GetMapping("/groups/external")
    public Set<String> searchExternalGroup(
            @Parameter(description = "mgmt.search-external-groups.param.search") @RequestParam("search") String searchTerm,
            @Parameter(description = "mgmt.search-external-groups.param.page-size") @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return extGroupService.findAllByName(searchTerm, pageSize)
                .stream()
                .map(org.openl.rules.security.Group::getName)
                .collect(StreamUtils.toTreeSet(String.CASE_INSENSITIVE_ORDER));
    }

    public static class UIGroup {
        private UIGroup(Group group) {
            id = group.getId();
            description = group.getDescription();
            privileges = group.getPrivileges();
            roles = group.getIncludedGroups()
                    .stream()
                    .map(Group::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        }

        @Parameter(description = "mgmt.schema.group.id")
        public Long id;

        @Parameter(description = "mgmt.schema.group.description")
        public String description;

        @Parameter(description = "mgmt.schema.group.sub-groups")
        public Set<String> roles;

        @Parameter(description = "mgmt.schema.group.privileges")
        public Set<String> privileges;
    }
}
