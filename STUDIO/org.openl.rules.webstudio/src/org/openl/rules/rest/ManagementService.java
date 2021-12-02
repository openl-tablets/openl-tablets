package org.openl.rules.rest;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
import org.springframework.stereotype.Service;

/**
 * Manages Users and Groups.
 * 
 * @author Yury Molchan
 */

@Service
@Path("/admin/management")
@Produces(MediaType.APPLICATION_JSON)
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

    @GET
    @Path("/groups")
    public Map<String, UIGroup> getGroups() {
        SecurityChecker.allow(Privileges.ADMIN);
        return groupDao.getAllGroups().stream().collect(StreamUtils.toLinkedMap(Group::getName, UIGroup::new));
    }

    @DELETE
    @Path("/groups")
    @Consumes(MediaType.TEXT_PLAIN)
    public void deleteGroup(final String name) {
        SecurityChecker.allow(Privileges.ADMIN);
        groupDao.deleteGroupByName(name);
    }

    @POST
    @Path("/groups")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void saveGroup(@FormParam("oldName") final String oldName,
            @FormParam("name") final String name,
            @FormParam("description") final String description,
            @FormParam("group") final Set<String> roles,
            @FormParam("privilege") final Set<String> privileges) {
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

    @POST
    @Path("/groups/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveSettings(GroupSettingsModel request) {
        SecurityChecker.allow(Privileges.ADMIN);
        validationProvider.validate(request);
        properties.setProperty(SECURITY_DEF_GROUP_PROP, request.getDefaultGroup());
    }

    @GET
    @Path("/groups/settings")
    public GroupSettingsModel getSettings() {
        GroupSettingsModel model = new GroupSettingsModel();
        model.setDefaultGroup(properties.getProperty(SECURITY_DEF_GROUP_PROP));
        return model;
    }

    @GET
    @Path("/privileges")
    public Map<String, String> getPrivileges() {
        SecurityChecker.allow(Privileges.ADMIN);
        return Arrays.stream(Privileges.values())
            .collect(StreamUtils.toLinkedMap(Privilege::getName, Privilege::getDisplayName));
    }

    @GET
    @Path("/groups/external")
    public Set<String> searchExternalGroup(@QueryParam("search") String searchTerm,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize) {
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
