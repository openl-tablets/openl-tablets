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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manages Users and Groups.
 * 
 * @author Yury Molchan
 */
@RestController
@RequestMapping(value = "/admin/management", produces = MediaType.APPLICATION_JSON_VALUE)
public class ManagementService {

    private static final String SECURITY_DEF_GROUP_PROP = "security.default-group";

    private final GroupDao groupDao;
    private final GroupManagementService groupManagementService;
    private final InMemoryProperties properties;
    private final BeanValidationProvider validationProvider;
    private final ExternalGroupService extGroupService;

    @Autowired
    public ManagementService(GroupDao groupDao,
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

    @GetMapping("/groups")
    public Map<String, UIGroup> getGroups() {
        SecurityChecker.allow(Privileges.ADMIN);
        return groupDao.getAllGroups().stream().collect(StreamUtils.toLinkedMap(Group::getName, UIGroup::new));
    }

    @DeleteMapping(value = "/groups", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteGroup(@RequestBody final String name) {
        SecurityChecker.allow(Privileges.ADMIN);
        groupDao.deleteGroupByName(name);
    }

    @PostMapping(value = "/groups", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void saveGroup(@RequestParam(value = "oldName", required = false) final String oldName,
            @RequestParam("name") final String name,
            @RequestParam(value = "description", required = false) final String description,
            @RequestParam(value = "group", required = false) final Set<String> roles,
            @RequestParam(value = "privilege", required = false) final Set<String> privileges) {
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

    @PostMapping(value = "/groups/settings", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void saveSettings(@RequestBody GroupSettingsModel request) {
        SecurityChecker.allow(Privileges.ADMIN);
        validationProvider.validate(request);
        properties.setProperty(SECURITY_DEF_GROUP_PROP, request.getDefaultGroup());
    }

    @GetMapping("/groups/settings")
    public GroupSettingsModel getSettings() {
        GroupSettingsModel model = new GroupSettingsModel();
        model.setDefaultGroup(properties.getProperty(SECURITY_DEF_GROUP_PROP));
        return model;
    }

    @GetMapping("/privileges")
    public Map<String, String> getPrivileges() {
        SecurityChecker.allow(Privileges.ADMIN);
        return Arrays.stream(Privileges.values())
            .collect(StreamUtils.toLinkedMap(Privilege::getName, Privilege::getDisplayName));
    }

    @GetMapping("/groups/external")
    public Set<String> searchExternalGroup(@RequestParam("search") String searchTerm,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return extGroupService.findAllByName(searchTerm, pageSize)
            .stream()
            .map(org.openl.rules.security.Group::getName)
            .collect(StreamUtils.toTreeSet(String.CASE_INSENSITIVE_ORDER));
    }

    public static class UIGroup {
        private UIGroup(Group group) {
            description = group.getDescription();
            privileges = group.getPrivileges();
            roles = group.getIncludedGroups()
                .stream()
                .map(org.openl.rules.security.standalone.persistence.Group::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        }

        public String description;
        public Set<String> roles;
        public Set<String> privileges;
    }
}
