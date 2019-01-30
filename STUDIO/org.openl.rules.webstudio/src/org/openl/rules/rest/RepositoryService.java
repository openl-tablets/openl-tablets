package org.openl.rules.rest;

import static org.openl.rules.security.AccessManager.isGranted;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.rules.security.Privileges;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

/*
 GET /projects                                       list of (Project Name, Last Version, Last Modified Date, Last Modified By, Status, Editor)
 GET /project/{Project Name}/[{version}]             (Project_Name.zip)
 POST /project/{Project Name}                        (Some_Project.zip, comments)
 POST /project                                       (Some_Project.zip, comments)
 POST /project                                       (Some_Project.zip)
 POST /lock_project/{Project Name}                   (ok, fail (already locked by ...))
 POST /unlock_project/{Project Name}                 (ok, fail)
 */
@Service
@Path("/repo/")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryService {
    private final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    @Autowired
    private MultiUserWorkspaceManager workspaceManager;

    /**
     * @return a list of project descriptions.
     */
    @GET
    @Path("projects")
    public Response getProjects() throws WorkspaceException {
        if (!isGranted(Privileges.VIEW_PROJECTS)) {
            return Response.status(Status.FORBIDDEN).entity("Doesn't have VIEW privilege").build();
        }
        Collection<? extends AProject> projects = getDesignTimeRepository().getProjects();
        List<ProjectDescription> result = new ArrayList<>(projects.size());
        for (AProject prj : projects) {
            ProjectDescription projectDescription = getProjectDescription(prj);
            result.add(projectDescription);
        }
        return Response.ok(new GenericEntity<List<ProjectDescription>>(result) {}).build();
    }

    /**
     * Returns the latest zipped project.
     * 
     * @param name a project name
     * @return a zipped project
     */
    @GET
    @Path("project/{name}")
    @Produces("application/zip")
    public Response getLastProject(@PathParam("name") String name) throws WorkspaceException {
        try {
            if (!isGranted(Privileges.VIEW_PROJECTS)) {
                return Response.status(Status.FORBIDDEN).entity("Doesn't have VIEW privilege").build();
            }
            FileItem fileItem = getRepository().read(getFileName(name));
            if (fileItem == null) {
                throw new FileNotFoundException("File '" + name + "' not found.");
            }
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
     */
    @GET
    @Path("project/{name}/{version}")
    @Produces("application/zip")
    public Response getProject(@PathParam("name") String name, @PathParam("version") String version) throws WorkspaceException {
        try {
            if (!isGranted(Privileges.VIEW_PROJECTS)) {
                return Response.status(Status.FORBIDDEN).entity("Doesn't have VIEW privilege").build();
            }
            FileItem fileItem = getRepository().readHistory(getFileName(name), version);
            if (fileItem == null) {
                throw new FileNotFoundException("File '" + name + "' not found.");
            }
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
     */
    @POST
    @Path("project/{name}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addProject(@Context UriInfo uriInfo,
            @PathParam("name") String name,
            @Multipart(value = "file") InputStream zipFile,
            @Multipart(value = "comment", required = false) String comment) throws WorkspaceException {
        File modifiedZip = null;
        FileInputStream modifiedZipStream = null;
        File originalZipFolder = null;
        try {
            originalZipFolder = Files.createTempDirectory("openl").toFile();
            ZipUtils.extractAll(zipFile, originalZipFolder);

            File rules = new File(originalZipFolder, "rules.xml");
            if (rules.exists()) {
                // Change project name in rules.xml.
                try {
                    XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer(false);
                    ProjectDescriptor projectDescriptor = serializer.deserialize(new FileInputStream(rules));
                    projectDescriptor.setName(name);
                    String modifiedRules = serializer.serialize(projectDescriptor);

                    IOUtils.copyAndClose(IOUtils.toInputStream(modifiedRules), new FileOutputStream(rules));
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }

            modifiedZip = File.createTempFile("project", ".zip");
            ZipUtils.archive(originalZipFolder, modifiedZip);
            modifiedZipStream = new FileInputStream(modifiedZip);

            return addProject(uriInfo.getPath(false), name, modifiedZipStream, modifiedZip.length(), comment);
        } catch (IOException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } finally {
            FileUtils.deleteQuietly(originalZipFolder);
            IOUtils.closeQuietly(modifiedZipStream);
            FileUtils.deleteQuietly(modifiedZip);
        }
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be
     * performed if the project in the design repository is not locked by other
     * user.
     *
     * @param zipFile a zipped project
     * @param comment a revision comment
     */
    @POST
    @Path("project")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addProject(@Context UriInfo uriInfo,
                               @Multipart(value = "file") File zipFile,
                               @Multipart(value = "comment", required = false) String comment) throws WorkspaceException {
        File zipFolder = null;
        try {
            // Temp folders
            zipFolder = Files.createTempDirectory("openl").toFile();
            // Unpack jar to a file system
            ZipUtils.extractAll(zipFile, zipFolder);

            // Renamed a project according to rules.xml
            File rules = new File(zipFolder, "rules.xml");
            String name = null;
            if (rules.exists()) {
                name = getProjectName(rules);
            }
            if (StringUtils.isBlank(name)) {
                return Response.status(Status.NOT_ACCEPTABLE).entity("The uploaded file does not contain Project Name in the rules.xml ").build();
            }

            return addProject(uriInfo.getPath(false) + "/" + StringTool.encodeURL(name), name, new FileInputStream(zipFile), zipFile.length(), comment);
        } catch (IOException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } finally {
            /* Clean up */
            FileUtils.deleteQuietly(zipFolder);
            FileUtils.deleteQuietly(zipFile);
        }
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be
     * performed if the project in the design repository is not locked by other
     * user.
     *
     * @param zipFile a zipped project
     */
    @POST
    @Path("project")
    public Response addProject(@Context UriInfo uriInfo, File zipFile) throws WorkspaceException {
        return addProject(uriInfo, zipFile, null);
    }

    private Response addProject(String uri, String name, InputStream zipFile, long zipSize, String comment) throws WorkspaceException {
        try {
            UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(getUser());
            if (userWorkspace.hasProject(name)) {
                if (!isGranted(Privileges.EDIT_PROJECTS)) {
                    return Response.status(Status.FORBIDDEN).entity("Doesn't have EDIT PROJECTS privilege").build();
                }
                RulesProject project = userWorkspace.getProject(name);
                if (project.isLocked() && !project.isLockedByUser(getUser())) {
                    String lockedBy = project.getLockInfo().getLockedBy().getUserName();
                    return Response.status(Status.FORBIDDEN).entity("Already locked by '" + lockedBy + "'").build();
                }
                project.lock();
            } else {
                if (!isGranted(Privileges.CREATE_PROJECTS)) {
                    return Response.status(Status.FORBIDDEN).entity("Doesn't have CREATE PROJECTS privilege").build();
                }
            }

            String fileName = getFileName(name);

            Repository repository = getRepository();
            FileData existing = repository.check(fileName);
            if (existing != null && existing.isDeleted()) {
                // Remove "deleted" marker
                FileData delData = new FileData();
                delData.setName(existing.getName());
                delData.setVersion(existing.getVersion());
                delData.setAuthor(existing.getAuthor());
                delData.setComment(Comments.restoreProject(name));
                repository.deleteHistory(delData);
            }

            FileData data = new FileData();
            data.setName(fileName);
            data.setComment("[REST] " + StringUtils.trimToEmpty(comment));
            data.setAuthor(getUserName());

            FileData save;
            if (repository instanceof FolderRepository) {
                try (ZipInputStream stream = new ZipInputStream(zipFile)) {
                    save = ((FolderRepository) repository).save(data, new FileChangesFromZip(stream, fileName));
                }
            } else {
                data.setSize(zipSize);
                save = repository.save(data, zipFile);
            }
            userWorkspace.getProject(name).unlock();
            return Response.created(new URI(uri + "/" + StringTool.encodeURL(save.getVersion()))).build();
        } catch (IOException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (URISyntaxException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (ProjectException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        } finally {
            IOUtils.closeQuietly(zipFile);
        }
    }

    private String getProjectName(File file) {
        try {
            InputSource inputSource = new InputSource(new FileInputStream(file));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/project/name");
            return StringUtils.trimToNull(xPathExpression.evaluate(inputSource));
        } catch (FileNotFoundException | XPathExpressionException e) {
            return null;
        }
    }

    private String getFileName(String name) throws WorkspaceException {
        return getDesignTimeRepository().getRulesLocation() + name;
    }

    /**
     * Locks the given project. The lock will be set if this project is not
     * locked.
     * 
     * @param name a project name to lock
     */
    @POST
    @Path("lockProject/{name}")
    public Response lockProject(@PathParam("name") String name) throws WorkspaceException, ProjectException {
        // When locking the project only EDIT_PROJECTS privilege is needed because we modify the project's state.
        if (!isGranted(Privileges.EDIT_PROJECTS)) {
            return Response.status(Status.FORBIDDEN).entity("Doesn't have EDIT PROJECTS privilege").build();
        }
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
     */
    @POST
    @Path("unlockProject/{name}")
    public Response unlockProject(@PathParam("name") String name) throws WorkspaceException, ProjectException {
        // When unlocking the project locked by current user, only EDIT_PROJECTS privilege is needed because we modify the project's state.
        // UNLOCK_PROJECTS privilege is needed only to unlock the project locked by other user (it's not our case).
        if (!isGranted(Privileges.EDIT_PROJECTS)) {
            return Response.status(Status.FORBIDDEN).entity("Doesn't have EDIT PROJECTS privilege").build();
        }
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
