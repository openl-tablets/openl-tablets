package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.web.CopyProjectTransformer;
import org.openl.rules.webstudio.web.repository.project.CustomTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.PredefinedTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.project.TemplatesResolver;
import org.openl.rules.webstudio.web.repository.upload.ProjectUploader;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.repositories.model.ProjectTemplateGroup;
import org.openl.studio.repositories.model.RepositoryFolder;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTypeService;
import org.openl.util.FileTypeHelper;
import org.openl.util.StringUtils;

/**
 * Creates projects the same way the legacy repository tab does, wrapping the reusable creation
 * primitives (template resolvers + {@link ExcelFilesProjectCreator}). The project is built in the
 * current user's workspace, granted a CONTRIBUTOR ACL, and the resulting file data is returned.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectCreationService {

    private static final String CUSTOM_TYPE = "custom";
    private static final String PREDEFINED_TYPE = "predefined";
    private static final String ROLLBACK_UPLOAD_COMMENT = "Rollback project upload.";

    private final AclProjectsHelper aclProjectsHelper;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final TagTypeService tagTypeService;
    private final TagService tagService;
    @Qualifier("zipFilter")
    private final PathFilter zipFilter;
    private final ZipCharsetDetector zipCharsetDetector;
    @Value("${openl.home:}")
    private final String openlHome;

    private final TemplatesResolver predefinedTemplatesResolver = new PredefinedTemplatesResolver();
    private TemplatesResolver customTemplatesResolver;

    @PostConstruct
    void init() {
        customTemplatesResolver = new CustomTemplatesResolver(openlHome);
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Lookup("commentService")
    protected Comments getCommentsService(String repoId) {
        return null;
    }

    private void requireCreatePermission(String repositoryId) {
        if (!aclProjectsHelper.hasCreateProjectPermission(repositoryId)) {
            throw new ForbiddenException("default.message");
        }
    }

    private void requireReadPermission(String repositoryId, String path) {
        if (!isReadable(repositoryId, path)) {
            throw new ForbiddenException("default.message");
        }
    }

    private boolean isReadable(String repositoryId, String path) {
        return aclServiceProvider.getDesignRepoAclService()
                .isGranted(repositoryId, aclPath(path), List.of(BasePermission.READ));
    }

    /**
     * Grants the current user the CONTRIBUTOR role on a freshly created project, unless an ACL already
     * exists. Shared with create paths that build the project outside this service.
     */
    public static void grantContributorAclIfAbsent(RepositoryAclService acl, AProject project) {
        if (!acl.hasAcl(project)) {
            acl.createAcl(project, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
        }
    }

    /**
     * Register the project's {@code tags.properties} tags in the tag catalog: for an extensible tag type,
     * a value not yet known is created. Values of fixed-value types and unknown tag types are left as-is
     * (they require an administrator). Mirrors the legacy tab so imported/created projects keep their tags.
     *
     * <p>Tags are collected from both the design and local copies so every create path is covered:
     * publishing a local project exposes them through the local copy, while an archive or copy create
     * exposes them through the design copy.
     */
    void registerExtensibleTags(RulesProject project) {
        registerExtensibleTags(collectProjectTags(project));
    }

    /**
     * Register tags from a design project without requiring it to be opened in the user's workspace.
     * Archive upload keeps projects closed, so the tag catalog must read the repository artefact directly.
     */
    public void registerExtensibleTags(AProject project) {
        registerExtensibleTags(new ProjectTags(project).getTags());
    }

    /**
     * Register tags from a freshly written design project and resync the user workspace with the design
     * repository. Tags are read from the repository artefact; workspace refresh keeps project list metadata
     * aligned for later open, branch, and save operations.
     */
    public void registerExtensibleTagsAfterDesignChange(AProject project) {
        registerExtensibleTags(project);
        getUserWorkspace().refresh();
    }

    /**
     * Register tags from a workspace project and resync the user workspace after a design-repository write.
     */
    public void registerExtensibleTagsAfterDesignChange(RulesProject project) {
        registerExtensibleTags(project);
        getUserWorkspace().refresh();
    }

    private void registerExtensibleTags(Map<String, String> tags) {
        tags.forEach((typeName, value) -> {
            TagType type = tagTypeService.getByName(typeName);
            if (type != null && type.isExtensible() && tagService.getByName(type.getId(), value) == null) {
                var tag = new Tag();
                tag.setType(type);
                tag.setName(value);
                tagService.save(tag);
            }
        });
    }

    private static Map<String, String> collectProjectTags(RulesProject project) {
        var tags = new HashMap<String, String>();
        try {
            tags.putAll(project.getDesignTags());
        } catch (RuntimeException e) {
            log.warn("Cannot read design tags of project '{}'", project.getName(), e);
        }
        try {
            tags.putAll(project.getLocalTags());
        } catch (RuntimeException e) {
            log.warn("Cannot read local tags of project '{}'", project.getName(), e);
        }
        return tags;
    }

    /** List the available predefined (bundled) and custom project templates, grouped by category. */
    public List<ProjectTemplateGroup> listTemplates() {
        var groups = new ArrayList<ProjectTemplateGroup>();
        addGroups(groups, PREDEFINED_TYPE, predefinedTemplatesResolver);
        addGroups(groups, CUSTOM_TYPE, customTemplatesResolver);
        return groups;
    }

    private static void addGroups(List<ProjectTemplateGroup> groups, String type, TemplatesResolver resolver) {
        if (resolver == null) {
            return;
        }
        for (String category : resolver.getCategories()) {
            var templates = resolver.getTemplates(category);
            if (!templates.isEmpty()) {
                groups.add(ProjectTemplateGroup.builder().type(type).category(category).templates(templates).build());
            }
        }
    }

    /**
     * Create a project from a bundled or custom template, granting the creator a CONTRIBUTOR ACL. The
     * template's Excel files become the project content.
     *
     * @return the created project's file data (branch/revision)
     */
    public FileData createFromTemplate(String repositoryId, String projectName, String path,
                                       String type, String category, String template,
                                       String comment, Map<String, String> tags) {
        requireCreatePermission(repositoryId);
        var resolver = CUSTOM_TYPE.equals(type) ? customTemplatesResolver : predefinedTemplatesResolver;
        ProjectFile[] files = resolver.getProjectFiles(category, template);
        if (files.length == 0) {
            throw new NotFoundException("project.template.not-found.message");
        }
        return createFromFiles(repositoryId, projectName, path, new ArrayList<>(List.of(files)), comment,
                "rules/Models.xlsx", "rules/Algorithms.xlsx", "Models", "Algorithms", tags);
    }

    /**
     * Create a project from uploaded files, granting the creator a CONTRIBUTOR ACL. The upload dispatcher
     * recognises the content by extension: a single ZIP archive, one or more Excel files, or a single
     * OpenAPI (Swagger) file (which builds a data-types module and a rules module at the given paths).
     *
     * @return the created project's file data (branch/revision)
     */
    public FileData createFromFiles(String repositoryId, String projectName, String path, List<ProjectFile> files,
                                    String comment, String modelsPath, String algorithmsPath, String modelsModuleName,
                                    String algorithmsModuleName, Map<String, String> tags) {
        requireCreatePermission(repositoryId);
        try {
            RulesProject created = new ProjectUploader(repositoryId, files, projectName, StringUtils.trimToEmpty(path),
                    getUserWorkspace(), aclServiceProvider.getDesignRepoAclService(), comment, zipFilter,
                    zipCharsetDetector, modelsPath, algorithmsPath, modelsModuleName, algorithmsModuleName,
                    tags != null ? tags : Map.of()).uploadProject();
            registerExtensibleTags(created);
            return created.getFileData();
        } catch (ProjectException e) {
            throw new ConflictException("project.create.failed.message");
        }
    }

    /**
     * Publish local workspace projects to a design repository, keeping each project's name and granting
     * the creator a CONTRIBUTOR ACL on every published project.
     */
    public void uploadLocalProjects(String repositoryId, List<String> names, String path, String comment) {
        requireCreatePermission(repositoryId);
        var workspace = getUserWorkspace();
        var designRepoAclService = aclServiceProvider.getDesignRepoAclService();
        var comments = getCommentsService(repositoryId);
        var uploaded = new ArrayList<RulesProject>();
        try {
            for (String name : names) {
                // Generate a default commit message when the user gave none, so the publish commit is never
                // empty (matching the archive and copy create paths).
                var resolvedComment = StringUtils.isNotBlank(comment) ? comment : comments.createProject(name);
                RulesProject project = workspace.uploadLocalProject(repositoryId, name, StringUtils.trimToEmpty(path), resolvedComment);
                uploaded.add(project);
                grantContributorAclIfAbsent(designRepoAclService, project);
                registerExtensibleTagsAfterDesignChange(project);
            }
        } catch (ProjectException e) {
            rollbackUploadedProjects(workspace, designRepoAclService, uploaded);
            throw new ConflictException("project.workspace.upload.failed.message");
        } catch (RuntimeException e) {
            rollbackUploadedProjects(workspace, designRepoAclService, uploaded);
            throw e;
        }
    }

    private static void rollbackUploadedProjects(UserWorkspace workspace,
                                                 RepositoryAclService designRepoAclService,
                                                 List<RulesProject> uploaded) {
        for (int i = uploaded.size() - 1; i >= 0; i--) {
            rollbackUploadedProject(workspace, designRepoAclService, uploaded.get(i));
        }
    }

    private static void rollbackUploadedProject(UserWorkspace workspace,
                                                RepositoryAclService designRepoAclService,
                                                RulesProject project) {
        var projectName = projectNameOf(project);
        try {
            project.delete(workspace.getUser(), ROLLBACK_UPLOAD_COMMENT);
        } catch (ProjectException | RuntimeException e) {
            log.warn("Cannot roll back published workspace project '{}'", projectName, e);
            return;
        }
        try {
            designRepoAclService.deleteAcl(project);
        } catch (RuntimeException e) {
            log.warn("Cannot delete ACL for rolled back workspace project '{}'", projectName, e);
        }
    }

    private static String projectNameOf(RulesProject project) {
        try {
            return project.getName();
        } catch (RuntimeException e) {
            return "<unknown>";
        }
    }

    /**
     * List the immediate sub-folders of a non-flat design repository, offered as candidates for the
     * "import from repository" flow. Folders already imported as a project (or nested in one) are
     * marked as mapped.
     *
     * @param path the internal folder whose children to list; blank lists the repository root
     */
    public List<RepositoryFolder> listImportableFolders(String repositoryId, String path) {
        requireCreatePermission(repositoryId);
        requireReadPermission(repositoryId, path);
        var mapper = mappedRepository(repositoryId);
        var delegate = mapper.getDelegate();
        try {
            var result = new ArrayList<RepositoryFolder>();
            for (FileData folder : delegate.listFolders(normalizeFolder(path))) {
                var name = folder.getName();
                if (isReadable(repositoryId, name)) {
                    result.add(toRepositoryFolder(mapper, name, isOpenLProject(delegate, name)));
                }
            }
            result.sort((left, right) -> left.name().compareToIgnoreCase(right.name()));
            return result;
        } catch (IOException e) {
            throw new ConflictException("project.import.list.failed.message");
        }
    }

    /** A folder holds an OpenL project when it contains a rules.xml descriptor or Excel workbooks. */
    private static boolean isOpenLProject(Repository delegate, String folderPath) throws IOException {
        return delegate.list(normalizeFolder(folderPath)).stream()
                .map(file -> file.getName().substring(file.getName().lastIndexOf('/') + 1))
                .anyMatch(name -> ProjectDescriptor.FILE_NAME.equals(name) || FileTypeHelper.isExcelFile(name));
    }

    /**
     * Import an existing folder of a non-flat design repository as a project, granting the creator a
     * CONTRIBUTOR ACL. The project keeps the folder's own name (from its descriptor, or the folder
     * name). Given tags replace the imported project's local tags.
     *
     * @return the imported project's file data (branch/revision)
     */
    public FileData importFromRepository(String repositoryId, String path) {
        requireCreatePermission(repositoryId);
        var folder = trimTrailingSlash(StringUtils.trimToEmpty(path));
        if (folder.isEmpty()) {
            throw new BadRequestException("project.import.path-required.message");
        }
        requireReadPermission(repositoryId, folder);
        var workspace = getUserWorkspace();
        var mapper = mappedRepository(repositoryId);
        boolean mappingAdded = false;
        String mappedName = null;
        try {
            if (mapper.getDelegate().check(folder) == null) {
                throw new NotFoundException("project.import.folder-not-found.message");
            }
            mapper.addMapping(folder);
            mappingAdded = true;
            mappedName = mapper.findMappedName(folder);
            workspace.refresh();
            RulesProject project = workspace.getProjectByPath(repositoryId, folder)
                    .orElseThrow(() -> new ConflictException("project.import.failed.message"));
            RepositoryAclService designRepoAclService = aclServiceProvider.getDesignRepoAclService();
            grantContributorAclIfAbsent(designRepoAclService, project);
            registerExtensibleTags(project);
            return project.getFileData();
        } catch (IOException e) {
            rollbackImportMapping(mapper, mappingAdded, mappedName, folder);
            throw new ConflictException("project.import.failed.message");
        } catch (RuntimeException e) {
            rollbackImportMapping(mapper, mappingAdded, mappedName, folder);
            throw e;
        }
    }

    private static void rollbackImportMapping(FolderMapper mapper, boolean mappingAdded, String mappedName,
                                              String folder) {
        if (!mappingAdded) {
            return;
        }
        try {
            var path = StringUtils.isNotBlank(mappedName) ? mappedName : folder;
            mapper.removeMapping(path);
        } catch (IOException e) {
            log.warn("Cannot roll back imported project mapping for folder '{}'", folder, e);
        }
    }

    /**
     * Copy an existing project into a design repository under a new name, entirely server-side (the
     * project's folder is copied in the repository, not downloaded and re-uploaded). The descriptor is
     * renamed, the creator is granted a CONTRIBUTOR ACL, and the workspace is refreshed so the copy is
     * indexed.
     *
     * @return the created copy's file data (branch/revision)
     */
    public FileData copyProject(String targetRepositoryId, String newName, String path,
                                String sourceRepositoryId, String sourceProjectName, String comment) {
        requireCreatePermission(targetRepositoryId);
        var workspace = getUserWorkspace();
        var designRepoAclService = aclServiceProvider.getDesignRepoAclService();
        try {
            RulesProject source = workspace.getProject(sourceRepositoryId, sourceProjectName, true);
            if (!designRepoAclService.isGranted(source, List.of(BasePermission.READ))) {
                throw new ForbiddenException("default.message");
            }
            var designTimeRepository = workspace.getDesignTimeRepository();
            Repository targetRepository = designTimeRepository.getRepository(targetRepositoryId);
            String designPath = designTimeRepository.getRulesLocation() + newName;
            var designData = new FileData();
            designData.setName(designPath);
            designData.setComment(comment);
            if (targetRepository.supports().mappedFolders()) {
                designData.addAdditionalData(new FileMappingData(designPath, copyInternalPath(path, newName)));
            }
            var user = workspace.getUser();
            var targetProject = new AProject(targetRepository, designData);
            var sourceCopy = new AProject(source.getRepository(), source.getFolderPath());
            targetProject.setResourceTransformer(new CopyProjectTransformer(newName, Map.of()));
            targetProject.update(sourceCopy, user);
            targetProject.setResourceTransformer(null);
            var copied = new RulesProject(user, workspace.getLocalWorkspace().getRepository(targetRepositoryId),
                    null, targetRepository, targetProject.getFileData(), workspace.getProjectsLockEngine());
            grantContributorAclIfAbsent(designRepoAclService, copied);
            registerExtensibleTagsAfterDesignChange(copied);
            return copied.getFileData();
        } catch (ProjectException e) {
            throw new ConflictException("project.copy.failed.message");
        }
    }

    private FolderMapper mappedRepository(String repositoryId) {
        Repository repository = getUserWorkspace().getDesignTimeRepository().getRepository(repositoryId);
        if (repository == null) {
            throw new NotFoundException("repository.not-found.message");
        }
        if (!repository.supports().mappedFolders()) {
            throw new ConflictException("project.import.flat-repository.message");
        }
        return (FolderMapper) repository;
    }

    private static RepositoryFolder toRepositoryFolder(FolderMapper mapper, String internalPath, boolean project) {
        var folder = trimTrailingSlash(internalPath);
        return RepositoryFolder.builder()
                .name(folder.substring(folder.lastIndexOf('/') + 1))
                .path(folder)
                .mapped(mapper.findMappedName(folder) != null ? Boolean.TRUE : null)
                .project(project ? Boolean.TRUE : null)
                .build();
    }

    private static String normalizeFolder(String path) {
        var folder = StringUtils.trimToEmpty(path);
        if (folder.isEmpty() || folder.endsWith("/")) {
            return folder;
        }
        return folder + "/";
    }

    private static String aclPath(String path) {
        var folder = trimTrailingSlash(StringUtils.trimToEmpty(path));
        return StringUtils.trimToNull(folder);
    }

    static String copyInternalPath(String path, String projectName) {
        var folder = StringUtils.trimToEmpty(path).replace('\\', '/');
        while (folder.startsWith("/")) {
            folder = folder.substring(1);
        }
        if (!folder.isEmpty() && !folder.endsWith("/")) {
            folder += "/";
        }
        return folder + projectName;
    }

    private static String trimTrailingSlash(String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
