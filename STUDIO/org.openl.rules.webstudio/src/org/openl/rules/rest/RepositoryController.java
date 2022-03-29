package org.openl.rules.rest;

import static org.openl.rules.security.AccessManager.isGranted;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.security.Privileges;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;

import io.swagger.v3.oas.annotations.tags.Tag;

/*
 GET /projects                                       list of (Project Name, Last Version, Last Modified Date, Last Modified By, Status, Editor)
 GET /project/{Project Name}/[{version}]             (Project_Name.zip)
 POST /project/{Project Name}                        (Some_Project.zip, comments)
 POST /project                                       (Some_Project.zip, comments)
 POST /project                                       (Some_Project.zip)
 POST /lock_project/{Project Name}                   (ok, fail (already locked by ...))
 POST /unlock_project/{Project Name}                 (ok, fail)
 */
@RestController
@RequestMapping(value = "/repo/", produces = MediaType.APPLICATION_JSON_VALUE)
@Deprecated
@Tag(name = "Design Repository")
public class RepositoryController {
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryController.class);

    private final MultiUserWorkspaceManager workspaceManager;

    private final UserManagementService userManagementService;

    private final Comments designRepoComments;

    @Autowired
    public RepositoryController(MultiUserWorkspaceManager workspaceManager,
            PropertyResolver propertyResolver,
            UserManagementService userManagementService) {
        this.workspaceManager = workspaceManager;
        this.designRepoComments = new Comments(propertyResolver, Comments.DESIGN_CONFIG_REPO_ID);
        this.userManagementService = userManagementService;
    }

    /**
     * @return a list of project descriptions.
     */
    @GetMapping("projects")
    public ResponseEntity<?> getProjects() {
        if (!isGranted(Privileges.VIEW_PROJECTS)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Does not have VIEW privilege");
        }
        Collection<? extends AProject> projects = getDesignTimeRepository().getProjects();
        List<ProjectDescription> result = new ArrayList<>(projects.size());
        for (AProject prj : projects) {
            ProjectDescription projectDescription = getProjectDescription(prj);
            result.add(projectDescription);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Returns the latest zipped project.
     *
     * @param name a project name
     * @return a zipped project
     */
    @GetMapping(value = "project/{name}", produces = "application/zip")
    public ResponseEntity<?> getLastProject(@PathVariable("name") String name) {
        try {
            SecurityChecker.allow(Privileges.VIEW_PROJECTS);
            FileData fileData = getRepository().check(getFileName(name));
            if (fileData == null) {
                throw new FileNotFoundException(String.format("Project '%s' is not found.", name));
            }

            return getProject(name, fileData.getVersion());
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    /**
     * Returns a zipped project.
     *
     * @param name a project name
     * @param version a project version
     * @return a zipped project
     */
    @GetMapping(value = "project/{name}/{version}", produces = "application/zip")
    public ResponseEntity<?> getProject(@PathVariable("name") final String name,
            @PathVariable("version") final String version) {
        try {
            SecurityChecker.allow(Privileges.VIEW_PROJECTS);

            final Repository repository = getRepository();
            final String projectPath = getFileName(name);

            Object entity;

            if (repository.supports().folders()) {
                FileData fileData = getRepository().check(getFileName(name));
                if (fileData == null) {
                    throw new FileNotFoundException(String.format("Project '%s' is not found.", name));
                }

                final String rulesPath = getDesignTimeRepository().getRulesLocation();

                entity = (StreamingOutput) out -> RepositoryUtils
                    .archive((FolderRepository) repository, rulesPath, name, version, out, null);
            } else {
                FileItem fileItem = repository.readHistory(projectPath, version);
                if (fileItem == null) {
                    throw new FileNotFoundException(String.format("File '%s' is not found.", name));
                }

                entity = fileItem.getStream();
            }
            String zipFileName = String.format("%s-%s.zip", name, version);

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;filename=\"" + zipFileName + "\"")
                .body(entity);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
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
    @PostMapping(value = "project/{name}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProject(HttpServletRequest request,
            @PathVariable("name") String name,
            @RequestParam(value = "file") MultipartFile zipFile,
            @RequestParam(value = "comment", required = false) String comment) {
        File modifiedZip = null;
        FileInputStream modifiedZipStream = null;
        File originalZipFolder = null;
        try {
            originalZipFolder = Files.createTempDirectory("openl").toFile();
            ZipUtils.extractAll(zipFile.getInputStream(), originalZipFolder);

            File rules = new File(originalZipFolder, "rules.xml");
            if (rules.exists()) {
                // Change project name in rules.xml.
                try {
                    XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
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

            return addProject(request.getRequestURL()
                .toString(), name, modifiedZipStream, modifiedZip.length(), comment);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
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
    @PostMapping(value = "project", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProject(HttpServletRequest request,
            @RequestParam(value = "file") MultipartFile zipFile,
            @RequestParam(value = "comment", required = false) String comment) {
        File zipFolder = null;
        ByteArrayOutputStream cachedStream = new ByteArrayOutputStream();
        try {
            IOUtils.copyAndClose(zipFile.getInputStream(), cachedStream);
            // Temp folders
            zipFolder = Files.createTempDirectory("openl").toFile();
            // Unpack jar to a file system
            ZipUtils.extractAll(new ByteArrayInputStream(cachedStream.toByteArray()), zipFolder);

            // Renamed a project according to rules.xml
            File rules = new File(zipFolder, "rules.xml");
            String name = null;
            if (rules.exists()) {
                name = getProjectName(rules);
            }
            if (StringUtils.isBlank(name)) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body("The uploaded file does not contain Project Name in the rules.xml ");
            }

            return addProject(request.getRequestURL().toString() + "/" + StringTool.encodeURL(name),
                name,
                new ByteArrayInputStream(cachedStream.toByteArray()),
                zipFile.getSize(),
                comment);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        } finally {
            /* Clean up */
            FileUtils.deleteQuietly(zipFolder);
        }
    }

    /**
     * Uploads a zipped project to a design repository. The upload will be performed if the project in the design
     * repository is not locked by other user.
     *
     * @param zipFile a zipped project
     */
    @PostMapping("project")
    public ResponseEntity<?> addProject(HttpServletRequest request, @RequestParam MultipartFile zipFile) {
        return addProject(request, zipFile, null);
    }

    private ResponseEntity<?> addProject(String uri, String name, InputStream zipFile, long zipSize, String comment) {
        try {
            if (getRepository().supports().branches()) {
                BranchRepository branchRepo = (BranchRepository) getRepository();
                if (branchRepo.isBranchProtected(branchRepo.getBranch())) {
                    throw new ForbiddenException("default.message");
                }
            }
            UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(getUser());
            String repositoryId = getDefaultRepositoryId();
            if (userWorkspace.hasProject(repositoryId, name)) {
                if (!isGranted(Privileges.EDIT_PROJECTS)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Does not have EDIT PROJECTS privilege");
                }
                RulesProject project = userWorkspace.getProject(repositoryId, name);
                if (!project.tryLock()) {
                    String lockedBy = project.getLockInfo().getLockedBy();
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Already locked by '" + lockedBy + "'");
                }
            } else {
                if (!isGranted(Privileges.CREATE_PROJECTS)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Does not have CREATE PROJECTS privilege");
                }
                if (getRepository().supports().mappedFolders()) {
                    throw new UnsupportedOperationException(
                        "Cannot create a project for repository with non-flat folder structure");
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
                delData.setComment(designRepoComments.restoreProject(name));
                repository.deleteHistory(delData);
            }

            FileData data = new FileData();
            data.setName(fileName);
            data.setComment("[REST] " + StringUtils.trimToEmpty(comment));
            data.setAuthor(getUser().getUserInfo());

            FileData save;
            if (repository.supports().folders()) {
                try (ZipInputStream stream = new ZipInputStream(zipFile)) {
                    save = ((FolderRepository) repository)
                        .save(data, new FileChangesFromZip(stream, fileName), ChangesetType.FULL);
                }
            } else {
                data.setSize(zipSize);
                save = repository.save(data, zipFile);
            }
            userWorkspace.getProject(repositoryId, name).unlock();
            return ResponseEntity.created(new URI(uri + "/" + StringTool.encodeURL(save.getVersion()))).build();
        } catch (IOException | URISyntaxException | RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        } catch (ProjectException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
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

    private String getFileName(String name) {
        return getDesignTimeRepository().getRulesLocation() + name;
    }

    /**
     * Locks the given project. The lock will be set if this project is not locked.
     *
     * @param name a project name to lock
     */
    @PostMapping("lockProject/{name}")
    public ResponseEntity<?> lockProject(@PathVariable("name") String name) throws ProjectException {
        // When locking the project only EDIT_PROJECTS privilege is needed because we modify the project's state.
        if (!isGranted(Privileges.EDIT_PROJECTS)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Does not have EDIT PROJECTS privilege");
        }
        RulesProject project = workspaceManager.getUserWorkspace(getUser()).getProject(getDefaultRepositoryId(), name);
        if (!project.tryLock()) {
            String lockedBy = project.getLockInfo().getLockedBy();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Already locked by '" + lockedBy + "'");
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Unlocks the given project. The unlock will be set if this project is locked by current user.
     *
     * @param name a project name to unlock.
     */
    @PostMapping("unlockProject/{name}")
    public ResponseEntity<?> unlockProject(@PathVariable("name") String name) throws ProjectException {
        // When unlocking the project locked by current user, only EDIT_PROJECTS privilege is needed because we modify
        // the project's state.
        // UNLOCK_PROJECTS privilege is needed only to unlock the project locked by other user (it's not our case).
        if (!isGranted(Privileges.EDIT_PROJECTS)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Does not have EDIT PROJECTS privilege");
        }
        RulesProject project = workspaceManager.getUserWorkspace(getUser()).getProject(getDefaultRepositoryId(), name);
        if (!project.isLocked()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("The project is not locked.");
        } else if (!project.isLockedByMe()) {
            String lockedBy = project.getLockInfo().getLockedBy();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Locked by '" + lockedBy + "'");
        }
        project.unlock();
        return ResponseEntity.ok().build();
    }

    private Repository getRepository() {
        return getDesignTimeRepository().getRepository(getDefaultRepositoryId());
    }

    @Deprecated
    private String getDefaultRepositoryId() {
        // We don't support several repositories for now. Use first repository.
        return getDesignTimeRepository().getRepositories().get(0).getId();
    }

    private DesignTimeRepository getDesignTimeRepository() {
        return workspaceManager.getUserWorkspace(getUser()).getDesignTimeRepository();
    }

    private ProjectDescription getProjectDescription(AProject project) {
        ProjectDescription description = new ProjectDescription();
        description.setName(project.getName());
        ProjectVersion version = project.getVersion();
        description.setVersion(version.getRevision());
        VersionInfo versionInfo = version.getVersionInfo();
        description.setModifiedBy(versionInfo.getCreatedBy());
        description.setEmailModifiedBy(versionInfo.getEmailCreatedBy());
        description.setModifiedAt(versionInfo.getCreatedAt());
        boolean locked = project.isLocked();
        description.setLocked(locked);
        if (locked) {
            LockInfo lockInfo = project.getLockInfo();
            description.setLockedBy(lockInfo.getLockedBy());
            description.setLockedAt(new Date(lockInfo.getLockedAt().toEpochMilli()));
        }
        return description;
    }

    private WorkspaceUserImpl getUser() {
        return new WorkspaceUserImpl(getUserName(),
            (username) -> Optional.ofNullable(userManagementService.getUser(username))
                .map(usr -> new UserInfo(usr.getUsername(), usr.getEmail(), usr.getDisplayName()))
                .orElse(null));
    }

    private String getUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
