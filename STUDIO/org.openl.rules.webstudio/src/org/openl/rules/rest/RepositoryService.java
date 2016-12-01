package org.openl.rules.rest;

import java.io.File;
import java.io.IOException;
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
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.openl.rules.workspace.uw.impl.ProjectExportHelper;
import org.openl.util.FileUtils;
import org.openl.util.StringTool;
import org.openl.util.ZipUtils;
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
        Collection<? extends AProject> projects = getRepository().getProjects();
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
        File zipFile = null;
        try {

            AProject project = getRepository().getProject(name);
            zipFile = ProjectExportHelper.export(getUser(), project);
            String zipFileName = String.format("%s.zip", project.getName());

            return Response.ok(zipFile)
                .header("Content-Disposition", "attachment;filename=\"" + StringTool.encodeURL(zipFileName) + "\"")
                .build();
        } catch (ProjectException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        } finally {
            FileUtils.deleteQuietly(zipFile);
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
    public Response getProject(@PathParam("name") String name, @PathParam("version") Integer version) throws WorkspaceException {
        File zipFile = null;
        try {
            AProject project = getRepository().getProject(name, new CommonVersionImpl(version));
            zipFile = ProjectExportHelper.export(getUser(), project);
            String zipFileName = String.format("%s-%s.zip", project.getName(), version);

            return Response.ok(zipFile)
                .header("Content-Disposition", "attachment;filename=\"" + StringTool.encodeURL(zipFileName) + "\"")
                .build();
        } catch (ProjectException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        } finally {
            FileUtils.deleteQuietly(zipFile);
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
            @Multipart(value = "file") File zipFile,
            @Multipart(value = "comment", required = false) String comment) throws WorkspaceException {
        String destPath = zipFile.getPath() + "fld";
        File zipFolder = new File(destPath);
        File workspaceLocation = new File(zipFile.getPath() + "wrk");
        workspaceLocation.mkdir();
        try {
            AProject project = getRepository().getProject(name);
            if (project.isLocked() && !project.isLockedByUser(getUser())) {
                String lockedBy = project.getLockInfo().getLockedBy().getUserName();
                return Response.status(Status.FORBIDDEN).entity("Already locked by '" + lockedBy + "'").build();
            }

            ZipUtils.extractAll(zipFile, zipFolder);

            ArtefactPathImpl path = new ArtefactPathImpl(name);
            LocalWorkspaceImpl workspace = new LocalWorkspaceImpl(getUser(), workspaceLocation, null, null);
            LocalRepository repository = new LocalRepository(workspaceLocation);
            AProject newProject = new AProject(repository, path.getStringValue(), true);
            newProject.setVersionComment(comment);

            project.update(newProject, getUser());// updateProject(null,

            return Response.noContent().build();
        } catch (IOException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (PropertyException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (ProjectException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        } finally {
            /* Clean up */
            FileUtils.deleteQuietly(zipFile);
            FileUtils.deleteQuietly(zipFolder);
            FileUtils.deleteQuietly(workspaceLocation);
        }
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
        project.lock(getUser());
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
        project.unlock(getUser());
        return Response.ok().build();
    }

    private DesignTimeRepository getRepository() throws WorkspaceException {
        return workspaceManager.getUserWorkspace(getUser()).getDesignTimeRepository();
    }

    private ProjectDescription getProjectDescription(AProject project) {
        ProjectDescription description = new ProjectDescription();
        description.setName(project.getName());
        description.setVersion(project.getVersionsCount() - 1);
        VersionInfo versionInfo = project.getVersion().getVersionInfo();
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName(); // get logged in username
        return new WorkspaceUserImpl(name);
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }
}
