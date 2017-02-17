package org.openl.rules.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/*
 GET /projects                                       list of (Project Name, Last Version, Last Modified Date, Last Modified By, Status, Editor)
 GET /project/{Project Name}/[{version}]             (Project_Name.zip)
 PUT /project/{Project Name}                         (Some_Project.zip, comments)
 POST /lock_project/{Project Name}                   (ok, fail (already locked by ...))
 POST /unlock_project/{Project Name}                 (ok, fail)
 */
@Service
@Path("/repo/")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryService {

    @Resource
    private MultiUserWorkspaceManager workspaceManager;

    /**
     * @return a list of project descriptions.
     * @throws WorkspaceException
     */
    @GET
    @Path("projects")
    public List<ProjectDescription> getProjects() throws WorkspaceException {
        Collection<? extends AProject> projects = getDesignTimeRepository().getProjects();
        List<ProjectDescription> result = new ArrayList<ProjectDescription>(projects.size());
        for (AProject prj : projects) {
            ProjectDescription projectDescription = getProjectDescription(prj);
            result.add(projectDescription);
        }
        return result;
    }

    /**
     * Returns the latest zipped project.
     * 
     * @param name a project name
     * @return a zipped project
     * @throws WorkspaceException
     */
    @GET
    @Path("project/{name}")
    @Produces("application/zip")
    public Response getLastProject(@PathParam("name") String name) throws WorkspaceException {
        try {
            FileItem fileItem = getRepository().read(getFileName(name));
            String zipFileName = String.format("%s-%s.zip", name, fileItem.getData().getVersion());

            return Response.ok(fileItem.getStream())
                    .header("Content-Disposition", "attachment;filename=\"" + zipFileName + "\"")
                    .build();
        } catch (IOException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
    }

    /**
     * Returns a zipped project.
     * 
     * @param name a project name
     * @param version a project version
     * @return a zipped project
     * @throws WorkspaceException
     */
    @GET
    @Path("project/{name}/{version:[0-9]+}")
    @Produces("application/zip")
    public Response getProject(@PathParam("name") String name, @PathParam("version") String version) throws WorkspaceException {
        try {
            FileItem fileItem = getRepository().readHistory(getFileName(name), version);
            String zipFileName = String.format("%s-%s.zip", name, version);

            return Response.ok(fileItem.getStream())
                .header("Content-Disposition", "attachment;filename=\"" + zipFileName + "\"")
                .build();
        } catch (IOException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be
     * performed if the project in the design repository is not locked by other
     * user.
     * 
     * @param name a project name to update
     * @param zipFile a zipped project
     * @param comment a revision comment
     * @return
     * @throws WorkspaceException
     */
    @PUT
    @Path("project/{name}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response putProject(@PathParam("name") String name,
            @Multipart(value = "file") InputStream zipFile,
            @Multipart(value = "comment", required = false) String comment) throws WorkspaceException {
        try {
            RulesProject project = workspaceManager.getUserWorkspace(getUser()).getProject(name);
            if (project.isLocked() && !project.isLockedByUser(getUser())) {
                String lockedBy = project.getLockInfo().getLockedBy().getUserName();
                return Response.status(Status.FORBIDDEN).entity("Already locked by '" + lockedBy + "'").build();
            }
            project.lock();

            FileData data = new FileData();
            data.setName(getFileName(name));
            data.setComment("[REST] " + (comment != null ? comment : ""));
            data.setAuthor(getUserName());
            getRepository().save(data, zipFile);
            return Response.noContent().build();
        } catch (IOException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (ProjectException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
    }

    private String getFileName(String name) {
        return "DESIGN/rules/" + name;
    }

    /**
     * Locks the given project. The lock will be set if this project is not
     * locked.
     * 
     * @param name a project name to lock
     * @return
     * @throws WorkspaceException
     * @throws ProjectException
     */
    @POST
    @Path("lockProject/{name}")
    public Response lockProject(@PathParam("name") String name) throws WorkspaceException, ProjectException {
        RulesProject project = workspaceManager.getUserWorkspace(getUser()).getProject(name);
        if (project.isLocked()) {
            String lockedBy = project.getLockInfo().getLockedBy().getUserName();
            return Response.status(Status.FORBIDDEN).entity("Already locked by '" + lockedBy + "'").build();
        }
        project.lock();
        return Response.ok().build();
    }

    /**
     * Unlocks the given project. The unlock will be set if this project is
     * locked by current user.
     * 
     * @param name a project name to unlock.
     * @return
     * @throws WorkspaceException
     * @throws ProjectException
     */
    @POST
    @Path("unlockProject/{name}")
    public Response unlockProject(@PathParam("name") String name) throws WorkspaceException, ProjectException {
        RulesProject project = workspaceManager.getUserWorkspace(getUser()).getProject(name);
        if (!project.isLocked()) {
            return Response.status(Status.FORBIDDEN).entity("The project is not locked.").build();
        } else if (!project.isLockedByMe()) {
            String lockedBy = project.getLockInfo().getLockedBy().getUserName();
            return Response.status(Status.FORBIDDEN).entity("Locked by '" + lockedBy + "'").build();
        }
        project.unlock();
        return Response.ok().build();
    }

    private Repository getRepository() throws WorkspaceException {
        return getDesignTimeRepository().getRepository();
    }

    private DesignTimeRepository getDesignTimeRepository() throws WorkspaceException {
        return workspaceManager.getUserWorkspace(getUser()).getDesignTimeRepository();
    }

    private ProjectDescription getProjectDescription(AProject project) {
        ProjectDescription description = new ProjectDescription();
        description.setName(project.getName());
        ProjectVersion version = project.getVersion();
        description.setVersion(version.getRevision());
        VersionInfo versionInfo = version.getVersionInfo();
        description.setModifiedBy(versionInfo.getCreatedBy());
        description.setModifiedAt(versionInfo.getCreatedAt());
        boolean locked = project.isLocked();
        description.setLocked(locked);
        if (locked) {
            LockInfo lockInfo = project.getLockInfo();
            description.setLockedBy(lockInfo.getLockedBy().getUserName());
            description.setLockedAt(lockInfo.getLockedAt());
        }
        return description;
    }

    private WorkspaceUserImpl getUser() {
        String name = getUserName();
        return new WorkspaceUserImpl(name);
    }

    private String getUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }
}
