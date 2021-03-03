package org.openl.rules.rest;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
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

    private final GroupDao groupDao;

    private final GroupManagementService groupManagementService;

    @Autowired
    public ManagementService(GroupDao groupDao, GroupManagementService groupManagementService) {
        this.groupDao = groupDao;
        this.groupManagementService = groupManagementService;
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
        if (StringUtils.isBlank(oldName)) {
            groupManagementService.addGroup(name, description);
        } else {
            groupManagementService.updateGroup(oldName, name, description);
        }
        groupManagementService.updateGroup(name, roles, privileges);
    }

    @GET
    @Path("/privileges")
    public Map<String, String> getPrivileges() {
        SecurityChecker.allow(Privileges.ADMIN);
        return Arrays.stream(Privileges.values())
            .collect(StreamUtils.toLinkedMap(Privilege::getName, Privilege::getDisplayName));
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
