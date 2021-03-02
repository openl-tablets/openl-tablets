package org.openl.rules.rest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/admin/management")
@Produces(MediaType.APPLICATION_JSON)
public class ManagementService {

    @Autowired
    private GroupDao groupDao;

    @GET
    @Path("/groups")
    public Map<String, UIGroup> getGroups() {
        return groupDao.getAllGroups().stream().collect(StreamUtils.toLinkedMap(Group::getName, UIGroup::new));
    }

    @DELETE
    @Path("/groups/{name}")
    public void deleteGroup(@PathParam("name") String name) {
        groupDao.deleteGroupByName(name);
    }

    @GET
    @Path("/privileges")
    public Map<String, String> getPrivileges() {
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
                .collect(Collectors.toList());

        }

        public final String description;
        public final Collection<String> roles;
        public final Collection<String> privileges;
    }
}
