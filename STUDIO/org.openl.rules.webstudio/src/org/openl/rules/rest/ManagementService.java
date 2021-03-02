package org.openl.rules.rest;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/admin/management")
@Produces(MediaType.APPLICATION_JSON)
public class ManagementService {

    @Autowired
    private GroupDao groupDao;
    @Autowired
    private GroupManagementService groupManagementService;

    @GET
    @Path("/groups")
    public Map<String, UIGroup> getGroups() {
        SecurityChecker.allow(Privileges.ADMIN);
        return groupDao.getAllGroups().stream().collect(StreamUtils.toLinkedMap(Group::getName, UIGroup::new));
    }

    @DELETE
    @Path("/groups/{name}")
    public void deleteGroup(@PathParam("name") final String name) {
        SecurityChecker.allow(Privileges.ADMIN);
        groupDao.deleteGroupByName(name);
    }

    @POST
    @Path("/groups/{name}")
    public void saveGroup(@PathParam("name") final String name, UIGroup group) {
        SecurityChecker.allow(Privileges.ADMIN);
        groupManagementService.updateGroup(name, group.name, group.description);
        groupManagementService.updateGroup(group.name, group.roles, group.privileges);
    }

    @POST
    @Path("/groups/")
    public void createGroup(UIGroup group) {
        SecurityChecker.allow(Privileges.ADMIN);
        groupManagementService.addGroup(group.name, group.description);
        groupManagementService.updateGroup(group.name, group.roles, group.privileges);
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

        public UIGroup() {
        }

        public String name;
        public String description;
        public Set<String> roles;
        public Set<String> privileges;
    }
}
