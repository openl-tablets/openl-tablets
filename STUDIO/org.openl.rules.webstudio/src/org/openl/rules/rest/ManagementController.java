package org.openl.rules.rest;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.security.acl.JdbcMutableAclService;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.security.AdminPrivilege;
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

    private final GroupDao groupDao;
    private final GroupManagementService groupManagementService;
    private final ExternalGroupService extGroupService;
    private final JdbcMutableAclService aclService;
    private final RepositoryAclServiceProvider aclServiceProvider;

    @Autowired
    public ManagementController(GroupDao groupDao,
                                GroupManagementService groupManagementService,
                                ExternalGroupService extGroupService,
                                @Autowired(required = false) JdbcMutableAclService aclService,
                                RepositoryAclServiceProvider aclServiceProvider) {
        this.groupDao = groupDao;
        this.groupManagementService = groupManagementService;
        this.extGroupService = extGroupService;
        this.aclService = aclService;
        this.aclServiceProvider = aclServiceProvider;
    }

    @Operation(description = "mgmt.get-groups.desc", summary = "mgmt.get-groups.summary")
    @GetMapping("/groups")
    @AdminPrivilege
    public Map<String, UIGroup> getGroups() {
        return groupDao.getAllGroups().stream()
                .collect(StreamUtils.toLinkedMap(Group::getName, group -> {
                    var uiGroup = new UIGroup(group);
                    buildnumberOfMembers(group, uiGroup);
                    return uiGroup;
                }));
    }

    private void buildnumberOfMembers(Group group, UIGroup uiGroup) {
        long internal = groupManagementService.countUsersInGroup(group.getName());
        long external = extGroupService.countUsersInGroup(group.getName());
        uiGroup.numberOfMembers = new NumberOfMembers(internal, external);
    }

    @Operation(description = "mgmt.delete-group.desc", summary = "mgmt.delete-group.summary")
    @DeleteMapping(value = "/groups/{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    @AdminPrivilege
    public void deleteGroup(@Parameter(description = "mgmt.schema.group.id") @PathVariable("id") final Long id) {
        Group group = groupDao.getGroupById(id);
        groupManagementService.deleteGroup(id);
        if (group != null && aclService != null) {
            aclService.deleteSid(new GrantedAuthoritySid(group.getName()));
        }
    }

    @DeleteMapping(value = "/groups")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    @AdminPrivilege
    @Hidden // for testing purposes
    public void deleteGroupByName(@RequestParam("name") final String name) {
        Group group = groupDao.getGroupByName(name);
        if (group != null) {
            groupManagementService.deleteGroup(group.getId());
            if (aclService != null) {
                aclService.deleteSid(new GrantedAuthoritySid(group.getName()));
            }
        }
    }

    @Operation(description = "mgmt.save-group.desc", summary = "mgmt.save-group.summary")
    @PostMapping(value = "/groups", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    @AdminPrivilege
    public void saveGroup(
            @Parameter(description = "mgmt.schema.group.old-name") @RequestParam(value = "oldName", required = false) final String oldName,
            @Parameter(description = "mgmt.schema.group.name", required = true) @RequestParam("name") final String name,
            @Parameter(description = "mgmt.schema.group.description") @RequestParam(value = "description", required = false) final String description,
            @Parameter(description = "mgmt.schema.group.design.role") @RequestParam(value = "designRole", required = false) final AclRole designRole,
            @Parameter(description = "mgmt.schema.group.prod.role") @RequestParam(value = "prodRole", required = false) final AclRole prodRole,
            @Parameter(description = "mgmt.schema.group.admin") @RequestParam(value = "admin", required = false) final Boolean admin) {
        if (!name.equals(oldName) && groupManagementService.existsByName(name)) {
            throw new ConflictException("duplicated.group.message");
        }
        if (StringUtils.isBlank(oldName)) {
            groupManagementService.addGroup(name, description);
        } else {
            groupManagementService.updateGroup(oldName, name, description);
        }
        if (Boolean.TRUE.equals(admin)) {
            groupManagementService.updateGroup(name, Collections.singleton(Privileges.ADMIN.getAuthority()));
        } else {
            groupManagementService.updateGroup(name, Collections.emptySet());
        }

        var sid = new GrantedAuthoritySid(name);
        var designRepoAclService = aclServiceProvider.getDesignRepoAclService();
        designRepoAclService.removeRootPermissions(sid);
        var prodRepoAclService = aclServiceProvider.getProdRepoAclService();
        prodRepoAclService.removeRootPermissions(sid);
        if (designRole != null) {
            designRepoAclService.addRootPermissions(sid, designRole.getCumulativePermission());
        }
        if (prodRole != null) {
            var prodPermission = prodRole.getCumulativePermission();
            prodRepoAclService.addRootPermissions(sid, prodPermission);
        }
    }

    @Operation(description = "mgmt.search-external-groups.desc", summary = "mgmt.search-external-groups.summary")
    @GetMapping("/groups/external")
    @AdminPrivilege
    public Set<String> searchExternalGroup(
            @Parameter(description = "mgmt.search-external-groups.param.search") @RequestParam("search") String searchTerm,
            @Parameter(description = "mgmt.search-external-groups.param.page-size") @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return extGroupService.findAllByName(searchTerm, pageSize)
                .stream()
                .map(org.openl.rules.security.Group::getAuthority)
                .collect(StreamUtils.toTreeSet(String.CASE_INSENSITIVE_ORDER));
    }

    public static class UIGroup {
        private UIGroup(Group group) {
            id = group.getId();
            description = group.getDescription();
            privileges = group.getPrivileges();
        }

        @Parameter(description = "mgmt.schema.group.id")
        public Long id;

        @Parameter(description = "mgmt.schema.group.description")
        public String description;

        @Parameter(description = "mgmt.schema.group.privileges")
        public Set<String> privileges;

        public NumberOfMembers numberOfMembers;
    }

    public static class NumberOfMembers {

        @Parameter(description = "mgmt.schema.group.internal-number-of-users")
        public final long internal;

        @Parameter(description = "mgmt.schema.group.external-number-of-users")
        public final long external;

        @Parameter(description = "mgmt.schema.group.total-number-of-users")
        public final long total;

        public NumberOfMembers(long internal, long external) {
            this.internal = internal;
            this.external = external;
            this.total = internal + external;
        }
    }
}
