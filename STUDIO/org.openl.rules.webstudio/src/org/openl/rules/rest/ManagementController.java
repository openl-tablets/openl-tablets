package org.openl.rules.rest;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.config.InMemoryProperties;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.model.GroupSettingsModel;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.util.StreamUtils;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

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

    private final GroupDao groupDao;
    private final GroupManagementService groupManagementService;
    private final InMemoryProperties properties;
    private final BeanValidationProvider validationProvider;
    private final ExternalGroupService extGroupService;

    @Autowired
    public ManagementController(GroupDao groupDao,
            GroupManagementService groupManagementService,
            InMemoryProperties properties,
            BeanValidationProvider validationProvider,
            ExternalGroupService extGroupService) {
        this.groupDao = groupDao;
        this.groupManagementService = groupManagementService;
        this.properties = properties;
        this.validationProvider = validationProvider;
        this.extGroupService = extGroupService;
    }

    @Operation(description = "mgmt.get-groups.desc", summary = "mgmt.get-groups.summary")
    @GetMapping("/groups")
    public Map<String, UIGroup> getGroups() {
        SecurityChecker.allow(Privileges.ADMIN);
        return groupDao.getAllGroups().stream().collect(StreamUtils.toLinkedMap(Group::getName, UIGroup::new));
    }

    @Operation(description = "mgmt.delete-group.desc", summary = "mgmt.delete-group.summary")
    @DeleteMapping(value = "/groups/{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteGroup(
            @Parameter(description = "mgmt.schema.group.id") @PathVariable("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        groupDao.deleteGroupById(id);
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
        groupManagementService.updateGroup(name, roles, privileges);
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

    @Operation(description = "mgmt.get-privileges.desc", summary = "mgmt.get-privileges.summary")
    @GetMapping("/privileges")
    public Map<String, String> getPrivileges() {
        SecurityChecker.allow(Privileges.ADMIN);
        return Arrays.stream(Privileges.values())
            .collect(StreamUtils.toLinkedMap(Privilege::getName, Privilege::getDisplayName));
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
                .map(org.openl.rules.security.standalone.persistence.Group::getName)
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
