package org.openl.rules.webstudio.web.repository.merge;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.xml.ProjectDescriptorSerializerFactory;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.MergeConflictException;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTypeHelper;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class MergeConflictBean {
    private final Logger log = LoggerFactory.getLogger(MergeConflictBean.class);

    private final MultiUserWorkspaceManager workspaceManager;

    private final Map<String, ConflictResolution> conflictResolutions = new HashMap<>();
    private final Map<String, Boolean> existInRepositoryCache = new HashMap<>();
    private String conflictedFile;
    private String mergeMessage;
    private boolean mergeMessageModified;
    private String mergeError;
    private String uploadError;

    public MergeConflictBean(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public List<ConflictGroup> getConflictGroups() {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return Collections.emptyList();
        }
        try {
            List<String> conflicts = new ArrayList<>(mergeConflict.getException().getConflictedFiles());
            Map<String, ConflictGroup> groups = new TreeMap<>((p1, p2) -> {
                if (p1.equals(p2)) {
                    return 0;
                } else {
                    // Put empty project name to be latest.
                    if (p1.isEmpty()) {
                        return 1;
                    }
                    if (p2.isEmpty()) {
                        return -1;
                    }
                    return p1.compareToIgnoreCase(p2);
                }
            });
            UserWorkspace userWorkspace = getUserWorkspace();
            String repositoryId = mergeConflict.getRepositoryId();
            for (String conflict : conflicts) {
                Optional<RulesProject> projectByPath = userWorkspace.getProjectByPath(repositoryId, conflict);
                String projectName;
                String projectPath;
                if (projectByPath.isPresent()) {
                    RulesProject project = projectByPath.get();
                    projectName = project.getName();
                    projectPath = project.getRealPath();
                } else {
                    projectName = "";
                    projectPath = "";
                }
                ConflictGroup group = groups.get(projectName);
                if (group == null) {
                    group = new ConflictGroup(projectName, projectPath);
                    groups.put(projectName, group);
                }
                group.addFile(conflict);
            }

            return new ArrayList<>(groups.values());
        } catch (WorkspaceException e) {
            return Collections.emptyList();
        }
    }

    public String getFileName(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }

        return path.substring(path.lastIndexOf('/') + 1);
    }

    public String getRealPath(String path) {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return path;
        }

        RulesProject project = mergeConflict.getProject();
        Repository repository;
        try {
            if (!mergeConflict.isMerging()) {
                repository = project.getDesignRepository();
            } else {
                String id = mergeConflict.getRepositoryId();
                repository = ((BranchRepository) getUserWorkspace().getDesignTimeRepository().getRepository(id))
                    .forBranch(mergeConflict.getMergeBranchTo());
            }
        } catch (WorkspaceException | IOException e) {
            log.error(e.getMessage(), e);
            return path;
        }
        if (repository.supports().mappedFolders()) {
            return ((FolderMapper) repository).getRealPath(path);
        }

        return path;
    }

    public String getYourCommit() {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return null;
        }
        return mergeConflict.isExportOperation() ? mergeConflict.getException().getTheirCommit()
                                                 : mergeConflict.getException().getYourCommit();
    }

    public String getTheirCommit() {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return null;
        }
        return mergeConflict.isExportOperation() ? mergeConflict.getException().getYourCommit()
                                                 : mergeConflict.getException().getTheirCommit();
    }

    public String getBaseCommit() {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        return mergeConflict == null ? null : mergeConflict.getException().getBaseCommit();
    }

    public String getYourRevision() {
        return getRevision(getYourCommit());
    }

    public String getTheirRevision() {
        return getRevision(getTheirCommit());
    }

    public String getBaseRevision() {
        return getRevision(getBaseCommit());
    }

    private String getRevision(String commit) {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return null;
        }

        try {
            UserWorkspace userWorkspace = getUserWorkspace();

            SimpleDateFormat formatter = new SimpleDateFormat(WebStudioFormats.getInstance().dateTime());
            Repository designRepository = userWorkspace.getDesignTimeRepository().getRepository(mergeConflict.getRepositoryId());
            for (String file : mergeConflict.getException().getConflictedFiles()) {
                if (designRepository.supports().mappedFolders()) {
                    designRepository = ((FolderMapper) designRepository).getDelegate();
                }
                FileData fileData = designRepository.checkHistory(file, commit);
                if (fileData != null) {
                    String modifiedOnStr = formatter.format(fileData.getModifiedAt());
                    return fileData.getAuthor() + ": " + modifiedOnStr;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        // Occur if the file in which the conflict did not exist in the checked commit. In this case return commit;
        return commit;
    }

    public String getYourBranch() {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return null;
        }
        return mergeConflict.isExportOperation() ? mergeConflict.getMergeBranchFrom()
                                                 : mergeConflict.getMergeBranchTo();
    }

    public String getTheirBranch() {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return null;
        }
        return mergeConflict.isExportOperation() ? mergeConflict.getMergeBranchTo()
                                                 : mergeConflict.getMergeBranchFrom();
    }

    public String getMergeMessage() {
        if (!mergeMessageModified) {
            mergeMessage = generateMergeMessage();
        }
        return mergeMessage;
    }

    public void setMergeMessage(String mergeMessage) {
        if (!Objects.equals(this.mergeMessage, mergeMessage)) {
            mergeMessageModified = true;
        }
        this.mergeMessage = mergeMessage;
    }

    public Map<String, ConflictResolution> getConflictResolutions() {
        if (conflictResolutions.isEmpty()) {
            MergeConflictInfo conflictException = ConflictUtils.getMergeConflict();
            if (conflictException != null) {
                for (String conflictedFile : conflictException.getException().getConflictedFiles()) {
                    conflictResolutions.put(conflictedFile, new ConflictResolution());
                }
            }
        }
        return conflictResolutions;
    }

    public void setCurrentConflictedFile(String conflictedFile) {
        this.conflictedFile = conflictedFile;
    }

    public void uploadListener(FileUploadEvent event) {
        try {
            conflictResolutions.get(conflictedFile).setCustomResolutionFile(new ProjectFile(event.getUploadedFile()));
            uploadError = null;
        } catch (IOException e) {
            uploadError = "Can't upload the file. " + e.getMessage();
        }
    }

    public void applyConflictResolution() {
        ConflictResolution conflictResolution = conflictResolutions.get(conflictedFile);
        if (conflictResolution.getCustomResolutionFile() == null) {
            uploadError = "You must upload the file";
            return;
        }
        conflictResolution.setResolutionType(ResolutionType.CUSTOM);
    }

    public void cancelConflictResolution() {
        ConflictResolution conflictResolution = conflictResolutions.get(conflictedFile);
        conflictResolution.setResolutionType(ResolutionType.UNRESOLVED);
        conflictResolution.setCustomResolutionFile(null);
    }

    public boolean isSaveDisabled() {
        for (Map.Entry<String, ConflictResolution> entry : conflictResolutions.entrySet()) {
            ConflictResolution resolution = entry.getValue();
            if (resolution.getResolutionType() == ResolutionType.UNRESOLVED) {
                return true;
            }

            if (resolution.getResolutionType() == ResolutionType.CUSTOM && resolution
                .getCustomResolutionFile() == null) {
                return true;
            }
        }

        return false;
    }

    public void saveAndResolve() {
        // Validate
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            mergeError = "Nothing to merge";
            return;
        }

        for (Map.Entry<String, ConflictResolution> entry : conflictResolutions.entrySet()) {
            ConflictResolution resolution = entry.getValue();
            if (resolution.getResolutionType() == ResolutionType.UNRESOLVED) {
                mergeError = String.format("You must resolve conflict for the file '%s'", entry.getKey());
                return;
            }

            if (resolution.getResolutionType() == ResolutionType.CUSTOM && resolution
                .getCustomResolutionFile() == null) {
                mergeError = String.format("You must upload your version of the file '%s'", entry.getKey());
                return;
            }
        }

        // Save
        List<FileItem> resolvedFiles = new ArrayList<>();
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject project = mergeConflict.getProject();
        boolean mergeOperation = mergeConflict.isMerging();
        String repositoryId = mergeConflict.getRepositoryId();
        try {
            if (!mergeOperation) {
                studio.freezeProject(project.getName());
            }
            boolean opened = project.isOpened();

            UserWorkspace userWorkspace = getUserWorkspace();

            Repository designRepository = userWorkspace.getDesignTimeRepository().getRepository(repositoryId);
            LocalRepository localRepository = userWorkspace.getLocalWorkspace().getRepository(repositoryId);

            for (Map.Entry<String, ConflictResolution> entry : conflictResolutions.entrySet()) {
                String name = entry.getKey();
                ConflictResolution conflictResolution = entry.getValue();

                FileItem file;
                InputStream stream;
                switch (conflictResolution.getResolutionType()) {
                    case YOURS:
                        if (mergeOperation) {
                            file = designRepository.readHistory(name, getYourCommit());
                        } else {
                            Optional<RulesProject> projectByPath = userWorkspace.getProjectByPath(repositoryId, name);
                            if (projectByPath.isPresent()) {
                                RulesProject p = projectByPath.get();
                                String artefactPath = name.substring(p.getRealPath().length() + 1);
                                String localName = p.getFolderPath() + "/" + artefactPath;
                                file = localRepository.read(localName);
                            } else {
                                file = null;
                            }
                        }
                        stream = file == null ? null : file.getStream();
                        resolvedFiles.add(new FileItem(name, stream));
                        break;
                    case THEIRS:
                        file = designRepository.readHistory(name, getTheirCommit());
                        stream = file == null ? null : file.getStream();
                        resolvedFiles.add(new FileItem(name, stream));
                        break;
                    case CUSTOM:
                        resolvedFiles.add(new FileItem(name, conflictResolution.getCustomResolutionFile().getInput()));
                        break;
                    default:
                        mergeError = "Cannot merge with resolution type " + conflictResolution.getResolutionType();
                        return;
                }
            }

            Map<String, List<Module>> modulesToAppend = findModulesToAppend(mergeConflict, resolvedFiles);

            ConflictResolveData conflictResolveData = new ConflictResolveData(mergeConflict.getException()
                .getTheirCommit(), resolvedFiles, mergeMessage);
            if (mergeOperation) {
                ((BranchRepository) designRepository).forBranch(mergeConflict.getMergeBranchTo())
                    .merge(mergeConflict.getMergeBranchFrom(),
                        userWorkspace.getUser().getUserId(),
                        conflictResolveData);
            } else {
                project.save(conflictResolveData);
            }

            String branch = mergeOperation ? mergeConflict.getMergeBranchTo() : project.getBranch();
            updateRulesXmlFiles(repositoryId, modulesToAppend, branch);

            if (opened) {
                if (project.isDeleted()) {
                    project.close();
                } else {
                    project.open();
                }
            }

            userWorkspace.refresh();
            studio.reset();
            clearMergeStatus();
        } catch (Exception e) {
            String message = "Failed to resolve conflict. See logs for details.";
            log.error(message, e);
            mergeError = message;
        } finally {
            for (FileItem file : resolvedFiles) {
                IOUtils.closeQuietly(file.getStream());
            }
            if (!mergeOperation) {
                studio.releaseProject(project.getName());
            }
        }
    }

    public void init() {
        try {
            clearConflictResolutions();
            ConflictUtils.saveResolutionsToSession(conflictResolutions);
            conflictedFile = null;

            mergeMessage = generateMergeMessage();
            mergeMessageModified = false;
            mergeError = null;
            uploadError = null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            mergeError = e.getMessage();
        }
    }

    public String getMergeError() {
        return mergeError;
    }

    public String getUploadError() {
        return uploadError;
    }

    private Map<String, List<Module>> findModulesToAppend(MergeConflictInfo mergeConflict,
        List<FileItem> resolvedFiles) throws WorkspaceException, IOException {
        UserWorkspace userWorkspace = getUserWorkspace();
        String repositoryId = mergeConflict.getRepositoryId();
        Map<String, List<Module>> modulesToAppend = new HashMap<>();
        for (FileItem resolvedFile : resolvedFiles) {
            String name = resolvedFile.getData().getName();
            if (!isExcelFile(name)) {
                continue;
            }
            if ((!hasYourFile(name) || !hasTheirFile(name)) && resolvedFile.getStream() != null) {
                Optional<RulesProject> projectByPath = userWorkspace.getProjectByPath(repositoryId, name);
                if (!projectByPath.isPresent()) {
                    continue;
                }

                RulesProject project = projectByPath.get();
                String projectPath = project.getRealPath();
                String rulesXmlFile = projectPath + "/rules.xml";
                if (hasYourFile(rulesXmlFile) && hasTheirFile(rulesXmlFile)) {
                    String moduleInternalPath = name.substring(projectPath.length() + 1);

                    IProjectDescriptorSerializer serializer = WebStudioUtils
                        .getBean(ProjectDescriptorSerializerFactory.class)
                        .getDefaultSerializer();
                    Repository repository = userWorkspace.getDesignTimeRepository().getRepository(repositoryId);

                    Module module;

                    FileItem fileItem = repository.readHistory(rulesXmlFile, getTheirCommit());
                    module = getModule(serializer, fileItem, moduleInternalPath);
                    if (module == null) {
                        if (mergeConflict.isMerging()) {
                            fileItem = repository.readHistory(rulesXmlFile, getYourCommit());
                        } else {
                            String artefactPath = name.substring(project.getRealPath().length() + 1);
                            String localName = project.getFolderPath() + "/" + artefactPath;

                            fileItem = userWorkspace.getLocalWorkspace().getRepository(repositoryId).read(localName);
                        }
                        module = getModule(serializer, fileItem, moduleInternalPath);
                    }

                    if (module != null) {
                        List<Module> modules = modulesToAppend.computeIfAbsent(projectPath, k -> new ArrayList<>());
                        modules.add(module);
                    }
                }
            }
        }
        return modulesToAppend;
    }

    private void updateRulesXmlFiles(String repositoryId, Map<String, List<Module>> modulesToAppend, String branch) throws WorkspaceException, IOException {
        // Update rules.xml files if needed after merge was successful.
        if (!modulesToAppend.isEmpty()) {
            Repository repository = getUserWorkspace().getDesignTimeRepository().getRepository(repositoryId);
            IProjectDescriptorSerializer serializer = WebStudioUtils.getBean(ProjectDescriptorSerializerFactory.class)
                .getDefaultSerializer();

            List<FileItem> files = new ArrayList<>();
            for (Map.Entry<String, List<Module>> entry : modulesToAppend.entrySet()) {
                String projectPath = entry.getKey();
                String rulesXmlFile = projectPath + "/rules.xml";
                try (FileItem fileItem = repository.read(rulesXmlFile)) {
                    if (fileItem != null) {
                        ProjectDescriptor descriptor = serializer.deserialize(fileItem.getStream());
                        Map<String, Module> modules = new LinkedHashMap<>();
                        modules.putAll(descriptor.getModules()
                            .stream()
                            .collect(Collectors.toMap(m -> m.getRulesRootPath().getPath(), m -> m)));
                        for (Module module : entry.getValue()) {
                            String path = module.getRulesRootPath().getPath();
                            // After merge there is possibility that there is no need to add a module.
                            if (!modules.containsKey(path)) {
                                modules.put(path, module);
                            }
                        }
                        descriptor.setModules(new ArrayList<>(modules.values()));
                        files.add(new FileItem(rulesXmlFile, IOUtils.toInputStream(serializer.serialize(descriptor))));
                    }
                }
            }

            if (!files.isEmpty()) {
                FileData folderData = new FileData();
                folderData.setName("");
                folderData.setAuthor(getUserWorkspace().getUser().getUserId());
                folderData.setComment(mergeMessage);
                folderData.setBranch(branch);
                ((FolderRepository) repository).save(folderData, files, ChangesetType.DIFF);
            }
        }
    }

    private Module getModule(IProjectDescriptorSerializer serializer,
            FileItem fileItem,
            String moduleInternalPath) throws IOException {
        try (InputStream stream = fileItem.getStream()) {
            ProjectDescriptor descriptor = serializer.deserialize(stream);
            for (Module module : descriptor.getModules()) {
                if (module.getRulesRootPath().getPath().equals(moduleInternalPath)) {
                    return module;
                }
            }
        }
        return null;
    }

    private String generateMergeMessage() {
        try {
            MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
            if (mergeConflict != null) {
                MergeConflictException exception = mergeConflict.getException();

                String rulesLocation = getRulesLocation();

                StringBuilder messageBuilder = new StringBuilder(
                    "Merge with commit " + exception.getTheirCommit() + "\nConflicts:");
                ArrayList<String> conflicts = new ArrayList<>(exception.getConflictedFiles());
                conflicts.sort(String.CASE_INSENSITIVE_ORDER);
                boolean merging = mergeConflict.isMerging();
                String yourBranch = getYourBranch();
                String theirBranch = getTheirBranch();
                Repository designRepository = getUserWorkspace().getDesignTimeRepository().getRepository(mergeConflict.getRepositoryId());
                for (String file : conflicts) {
                    ConflictResolution resolution = conflictResolutions.get(file);

                    // In non-flat repositories we should see full path. In flat repos only essential part.
                    if (!designRepository.supports().mappedFolders()) {
                        if (file.startsWith(rulesLocation)) {
                            file = file.substring(rulesLocation.length());
                        }
                    }
                    messageBuilder.append("\n\t").append(file);

                    if (resolution != null) {
                        ResolutionType resolutionType = resolution.getResolutionType();
                        if (resolutionType != ResolutionType.UNRESOLVED) {
                            String chosen = resolutionType.name().toLowerCase();
                            if (merging) {
                                switch (resolutionType) {
                                    case YOURS:
                                        chosen = yourBranch;
                                        break;
                                    case THEIRS:
                                        chosen = theirBranch;
                                        break;
                                }
                            }
                            messageBuilder.append(" (").append(chosen).append(')');
                        }
                    }
                }
                return messageBuilder.toString();
            }
        } catch (Exception e) {
            log.error("Cannot generate merge message. Will use empty string.", e);
        }

        return null;
    }

    @PreDestroy
    public void destroy() {
        try {
            clearMergeStatus();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void clearMergeStatus() {
        ConflictUtils.removeMergeConflict();
        clearConflictResolutions();
        existInRepositoryCache.clear();
        conflictedFile = null;
        mergeMessage = null;
        mergeMessageModified = false;
    }

    private void clearConflictResolutions() {
        for (ConflictResolution resolution : conflictResolutions.values()) {
            resolution.destroy();
        }
        conflictResolutions.clear();
    }

    public boolean isExcelFile(String file) {
        return FileTypeHelper.isExcelFile(file);
    }

    private boolean hasLocalFile(String repositoryId, String name) {
        try {
            return getUserWorkspace().getProjectByPath(repositoryId, name)
                .map(project -> project.hasArtefact(name.substring(project.getRealPath().length() + 1)))
                .orElse(false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean hasYourFile(String conflictedFile) {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return false;
        }

        String repositoryId = mergeConflict.getRepositoryId();

        boolean merging = mergeConflict.isMerging();
        if (merging) {
            return hasRepositoryFile(repositoryId, conflictedFile, getYourCommit());
        } else {
            return hasLocalFile(repositoryId, conflictedFile);
        }
    }

    public boolean isMerging() {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        return mergeConflict != null && mergeConflict.isMerging();
    }

    public boolean hasTheirFile(String conflictedFile) {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return false;
        }

        String repositoryId = mergeConflict.getRepositoryId();
        return hasRepositoryFile(repositoryId, conflictedFile, getTheirCommit());
    }

    public boolean hasBaseFile(String conflictedFile) {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict == null) {
            return false;
        }

        String repositoryId = mergeConflict.getRepositoryId();
        return hasRepositoryFile(repositoryId, conflictedFile, mergeConflict.getException().getBaseCommit());
    }

    public String getRepositoryId() {
        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        return mergeConflict == null ? null : mergeConflict.getRepositoryId();

    }

    private boolean hasRepositoryFile(String repositoryId, String name, String version) {
        try {
            // ':' is forbidden character in name, so it can be used as a separator.
            String key = name + ":" + version;
            Boolean value = existInRepositoryCache.get(key);
            if (value == null) {
                // Exist status is cached to make UI smoother
                value = getUserWorkspace().getDesignTimeRepository()
                    .getRepository(repositoryId)
                    .checkHistory(name, version) != null;
                existInRepositoryCache.put(key, value);
            }

            return value;
        } catch (WorkspaceException | IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private UserWorkspace getUserWorkspace() throws WorkspaceException {
        WorkspaceUser user = new WorkspaceUserImpl(SecurityContextHolder.getContext().getAuthentication().getName());
        return workspaceManager.getUserWorkspace(user);
    }

    private String getRulesLocation() throws WorkspaceException {
        UserWorkspace userWorkspace = getUserWorkspace();
        return userWorkspace.getDesignTimeRepository().getRulesLocation();
    }

}
