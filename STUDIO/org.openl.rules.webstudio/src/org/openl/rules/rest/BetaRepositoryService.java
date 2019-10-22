package org.openl.rules.rest;

import static org.openl.rules.security.AccessManager.isGranted;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
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
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.rules.security.Privileges;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Path("/beta/repo/")
@Produces(MediaType.APPLICATION_JSON)
public class BetaRepositoryService {
    private static final Logger LOG = LoggerFactory.getLogger(BetaRepositoryService.class);

    private final MultiUserWorkspaceManager workspaceManager;

    private final Comments designRepoComments;

    public BetaRepositoryService(MultiUserWorkspaceManager workspaceManager,
            @Qualifier("designRepositoryComments") Comments designRepoComments) {
        this.workspaceManager = workspaceManager;
        this.designRepoComments = designRepoComments;
    }

    /**
     * @return a list of project descriptions.
     */
    @GET
    @Path("projects")
    public Response getProjects() throws WorkspaceException {
        if (!isGranted(Privileges.VIEW_PROJECTS)) {
            return Response.status(Status.FORBIDDEN).entity("Does not have VIEW privilege").build();
        }
        Collection<? extends AProject> projects = getDesignTimeRepository().getProjects();
        List<ProjectDescription> result = new ArrayList<>(projects.size());
        for (AProject prj : projects) {
            ProjectDescription projectDescription = getProjectDescription(prj);
            result.add(projectDescription);
        }
        return Response.ok(new GenericEntity<List<ProjectDescription>>(result) {
        }).build();
    }

    /**
     * @return a list of all branches.
     */
    @GET
    @Path("branches")
    public Response getBranches() throws Exception {
        return getBranches(null);
    }

    /**
     * @return a list of branches for a specified project.
     */
    @GET
    @Path("branches/{name}")
    public Response getBranches(@PathParam("name") String name) throws Exception {
        if (!isGranted(Privileges.VIEW_PROJECTS)) {
            return Response.status(Status.FORBIDDEN).entity("Does not have VIEW privilege").build();
        }
        Repository repository = getRepository();
        if (!repository.supports().branches()) {
            return Response.status(Status.METHOD_NOT_ALLOWED)
                .entity("This repository does not support 'branches' feature")
                .build();
        }
        List<String> branches = ((BranchRepository) repository).getBranches(name);
        return Response.ok(new GenericEntity<List<String>>(branches) {
        }).build();
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
                return Response.status(Status.FORBIDDEN).entity("Does not have VIEW privilege").build();
            }
            FileData fileData = getRepository().check(getFileName(name));
            if (fileData == null) {
                throw new FileNotFoundException(String.format("Project '%s' is not found.", name));
            }

            return getProject(name, fileData.getVersion());
        } catch (IOException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
    }

    /**
     * Returns a zipped project.
     *
     * @param name a project name
     * @param rev a project version
     * @return a zipped project
     */
    @GET
    @Path("project/{name}/{rev: (.+)}")
    @Produces("application/zip")
    public Response getProject(@PathParam("name") final String name,
            @PathParam("rev") String rev) throws WorkspaceException {
        try {
            if (!isGranted(Privileges.VIEW_PROJECTS)) {
                return Response.status(Status.FORBIDDEN).entity("Does not have VIEW privilege").build();
            }

            Repository repo = getRepository();
            if (repo.supports().branches()) {
                BranchRepository brRepo = (BranchRepository) repo;
                List<String> branches = brRepo.getBranches(name);
                if (branches.contains(rev)) {
                    repo = brRepo.forBranch(rev);

                    FileData fileData = repo.check(getFileName(name));
                    if (fileData == null) {
                        throw new FileNotFoundException(String.format("Project '%s' is not found.", name));
                    }
                    rev = fileData.getVersion();
                }
            }

            final Repository repository = repo;
            final String version = rev;

            Object entity;

            if (repository.supports().folders()) {
                FileData fileData = repository.check(getFileName(name));
                if (fileData == null) {
                    throw new FileNotFoundException(String.format("Project '%s' is not found.", name));
                }

                final String rulesPath = getDesignTimeRepository().getRulesLocation();

                entity = new StreamingOutput() {
                    @Override
                    public void write(OutputStream out) throws IOException {
                        RepositoryUtils.archive((FolderRepository) repository, rulesPath, name, version, out);
                    }
                };
            } else {
                final String projectPath = getFileName(name);
                FileItem fileItem = repository.readHistory(projectPath, version);
                if (fileItem == null) {
                    throw new FileNotFoundException(String.format("File '%s' is not found.", name));
                }

                entity = fileItem.getStream();
            }
            String zipFileName = String.format("%s-%s.zip", name, version);

            return Response.ok(entity)
                .header("Content-Disposition", "attachment;filename=\"" + zipFileName + "\"")
                .build();
        } catch (IOException ex) {
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        }
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be performed if the project in the design
     * repository is not locked by other user.
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
        return addProject(uriInfo, name, null, zipFile, comment);
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be performed if the project in the design
     * repository is not locked by other user.
     *
     * @param name a project name to update
     * @param zipFile a zipped project
     * @param comment a revision comment
     */
    @POST
    @Path("project/{name}/{branch: (.+)}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addProject(@Context UriInfo uriInfo,
            @PathParam("name") String name,
            @PathParam("branch") String branch,
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
                    LOG.warn(e.getMessage(), e);
                }
            }

            modifiedZip = File.createTempFile("project", ".zip");
            ZipUtils.archive(originalZipFolder, modifiedZip);
            modifiedZipStream = new FileInputStream(modifiedZip);

            return addProject(uriInfo.getPath(false), name, branch, modifiedZipStream, modifiedZip.length(), comment);
        } catch (IOException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } finally {
            FileUtils.deleteQuietly(originalZipFolder);
            IOUtils.closeQuietly(modifiedZipStream);
            FileUtils.deleteQuietly(modifiedZip);
        }
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be performed if the project in the design
     * repository is not locked by other user.
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
        return addProject(uriInfo, null, zipFile, comment);
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be performed if the project in the design
     * repository is not locked by other user.
     *
     * @param zipFile a zipped project
     * @param comment a revision comment
     */
    @POST
    @Path("project/{branch: (.+)}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addProject(@Context UriInfo uriInfo,
            @PathParam("branch") String branch,
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
                return Response.status(Status.NOT_ACCEPTABLE)
                    .entity("The uploaded file does not contain Project Name in the rules.xml ")
                    .build();
            }

            return addProject(uriInfo.getPath(false) + "/" + StringTool.encodeURL(name),
                name,
                branch,
                new FileInputStream(zipFile),
                zipFile.length(),
                comment);
        } catch (IOException ex) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } finally {
            /* Clean up */
            FileUtils.deleteQuietly(zipFolder);
            FileUtils.deleteQuietly(zipFile);
        }
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be performed if the project in the design
     * repository is not locked by other user.
     *
     * @param zipFile a zipped project
     */
    @POST
    @Path("project")
    public Response addProject(@Context UriInfo uriInfo, File zipFile) throws WorkspaceException {
        return addProject(uriInfo, zipFile, null);
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be performed if the project in the design
     * repository is not locked by other user.
     *
     * @param zipFile a zipped project
     */
    @POST
    @Path("project/{branch: (.+)}")
    public Response addProject(@Context UriInfo uriInfo,
            @PathParam("branch") String branch,
            File zipFile) throws WorkspaceException {
        return addProject(uriInfo, branch, zipFile, null);
    }

    private Response addProject(String uri,
            String name,
            String branch,
            InputStream zipFile,
            long zipSize,
            String comment) throws WorkspaceException {
        try {
            UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(getUser());
            if (userWorkspace.hasProject(name)) {
                if (!isGranted(Privileges.EDIT_PROJECTS)) {
                    return Response.status(Status.FORBIDDEN).entity("Does not have EDIT PROJECTS privilege").build();
                }
                RulesProject project = userWorkspace.getProject(name);
                if (project.isLocked() && !project.isLockedByUser(getUser())) {
                    String lockedBy = project.getLockInfo().getLockedBy().getUserName();
                    return Response.status(Status.FORBIDDEN).entity("Already locked by '" + lockedBy + "'").build();
                }
                project.lock();
            } else {
                if (!isGranted(Privileges.CREATE_PROJECTS)) {
                    return Response.status(Status.FORBIDDEN).entity("Does not have CREATE PROJECTS privilege").build();
                }
                if (getRepository().supports().mappedFolders()) {
                    throw new UnsupportedOperationException(
                        "Cannot create a project for repository with non-flat folder structure");
                }
            }

            String fileName = getFileName(name);

            Repository repo = getRepository();

            if (branch != null && repo.supports().branches()) {
                BranchRepository brRepo = (BranchRepository) repo;
                List<String> branches = brRepo.getBranches(name);
                if (branches.contains(branch)) {
                    repo = brRepo.forBranch(branch);
                }
            }

            FileData existing = repo.check(fileName);
            if (existing != null && existing.isDeleted()) {
                // Remove "deleted" marker
                FileData delData = new FileData();
                delData.setName(existing.getName());
                delData.setVersion(existing.getVersion());
                delData.setBranch(branch);
                delData.setAuthor(existing.getAuthor());
                delData.setComment(designRepoComments.restoreProject(name));
                repo.deleteHistory(delData);
            }

            FileData data = new FileData();
            data.setName(fileName);
            data.setComment(comment == null ? designRepoComments.saveProject(name) : comment.trim());
            data.setAuthor(getUserName());
            data.setBranch(branch);

            FileData save;
            if (repo.supports().folders()) {
                try (ZipInputStream stream = new ZipInputStream(zipFile)) {
                    save = ((FolderRepository) repo)
                        .save(data, new FileChangesFromZip(stream, fileName), ChangesetType.FULL);
                }
            } else {
                data.setSize(zipSize);
                save = repo.save(data, zipFile);
            }
            userWorkspace.getProject(name).unlock();
            return Response.created(new URI(uri + "/" + StringTool.encodeURL(save.getVersion()))).build();
        } catch (IOException | URISyntaxException | RuntimeException ex) {
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
     * Locks the given project. The lock will be set if this project is not locked.
     *
     * @param name a project name to lock
     */
    @POST
    @Path("lockProject/{name}")
    public Response lockProject(@PathParam("name") String name) throws WorkspaceException, ProjectException {
        return lockProject(name, null);
    }

    /**
     * Locks the given project. The lock will be set if this project is not locked.
     *
     * @param name a project name to lock
     */
    @POST
    @Path("lockProject/{name}/{branch}")
    public Response lockProject(@PathParam("name") String name,
            @PathParam("branch") String branch) throws WorkspaceException, ProjectException {
        // When locking the project only EDIT_PROJECTS privilege is needed because we modify the project's state.
        if (!isGranted(Privileges.EDIT_PROJECTS)) {
            return Response.status(Status.FORBIDDEN).entity("Does not have EDIT PROJECTS privilege").build();
        }
        LockEngine lockEngine = workspaceManager.getUserWorkspace(getUser()).getProjectsLockEngine();
        LockInfo lockInfo = lockEngine.getLockInfo(branch, name);
        if (lockInfo.isLocked()) {
            String lockedBy = lockInfo.getLockedBy().getUserName();
            return Response.status(Status.FORBIDDEN).entity("Already locked by '" + lockedBy + "'").build();
        }
        lockEngine.tryLock(branch, name, getUserName());
        return Response.ok().build();
    }

    /**
     * Unlocks the given project. The unlock will be set if this project is locked by current user.
     *
     * @param name a project name to unlock.
     */
    @POST
    @Path("unlockProject/{name}")
    public Response unlockProject(@PathParam("name") String name) throws WorkspaceException, ProjectException {
        return unlockProject(name, null);
    }

    /**
     * Unlocks the given project. The unlock will be set if this project is locked by current user.
     *
     * @param name a project name to unlock.
     */
    @POST
    @Path("unlockProject/{name}/{branch}")
    public Response unlockProject(@PathParam("name") String name,
            @PathParam("branch") String branch) throws WorkspaceException, ProjectException {
        // When unlocking the project locked by current user, only EDIT_PROJECTS privilege is needed because we modify
        // the project's state.
        // UNLOCK_PROJECTS privilege is needed only to unlock the project locked by other user (it's not our case).
        if (!isGranted(Privileges.EDIT_PROJECTS)) {
            return Response.status(Status.FORBIDDEN).entity("Does not have EDIT PROJECTS privilege").build();
        }
        LockEngine lockEngine = workspaceManager.getUserWorkspace(getUser()).getProjectsLockEngine();
        LockInfo lockInfo = lockEngine.getLockInfo(branch, name);

        if (!lockInfo.isLocked()) {
            return Response.status(Status.FORBIDDEN).entity("The project is not locked.").build();
        } else if (!getUserName().equals(lockInfo.getLockedBy().getUserName())) {
            String lockedBy = lockInfo.getLockedBy().getUserName();
            return Response.status(Status.FORBIDDEN).entity("Locked by '" + lockedBy + "'").build();
        }
        lockEngine.unlock(branch, name);
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

    private ProjectDescription getProjectDescription(FileData project) {
        ProjectDescription description = new ProjectDescription();
        description.setName(project.getName());
        description.setVersion(project.getVersion());
        description.setModifiedBy(project.getAuthor());
        description.setModifiedAt(project.getModifiedAt());
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
}
