package org.openl.studio.projects.service.merge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.xls.merge.XlsWorkbookMerger;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.ConflictResolutionStatus;
import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;
import org.openl.util.FileTypeHelper;
import org.openl.util.IOUtils;

@Service
public class ProjectsMergeConflictsServiceImpl implements ProjectsMergeConflictsService {

    private static final Logger log = LoggerFactory.getLogger(ProjectsMergeConflictsServiceImpl.class);
    private static final XmlProjectDescriptorSerializer PROJECT_DESCRIPTOR_SERIALIZER = new XmlProjectDescriptorSerializer();

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Override
    public List<ConflictGroup> getMergeConflicts(MergeConflictInfo mergeConflictInfo) {
        if (mergeConflictInfo.details() == null || mergeConflictInfo.details().getConflictedFiles().isEmpty()) {
            return List.of();
        }
        List<String> conflicts = new ArrayList<>(mergeConflictInfo.details().getConflictedFiles());
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
        var workspace = getUserWorkspace();
        String repositoryId = mergeConflictInfo.getRepositoryId();
        for (String conflict : conflicts) {
            Optional<RulesProject> projectByPath = workspace.getProjectByPath(repositoryId, conflict);
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
            var group = groups.computeIfAbsent(projectName, n -> new ConflictGroup(n, projectPath));
            group.addFile(conflict);
        }

        return new ArrayList<>(groups.values());
    }

    @Override
    public FileItem getConflictFileItem(MergeConflictInfo mergeConflict, String path, ConflictBase side) throws IOException {
        var conflictDetails = mergeConflict.details();
        if (!conflictDetails.getConflictedFiles().contains(path)) {
            throw new NotFoundException("project.merge.conflict.file.not.found", path);
        }
        var repository = getRepository(mergeConflict);
        var realPath = getRealPath(repository, path);
        var commitRev = switch (side) {
            case BASE -> conflictDetails.baseCommit();
            case OURS -> mergeConflict.isExportOperation()
                    ? conflictDetails.theirCommit()
                    : conflictDetails.yourCommit();
            case THEIRS -> mergeConflict.isExportOperation()
                    ? conflictDetails.yourCommit()
                    : conflictDetails.theirCommit();
        };
        var fileItem = repository.readHistory(realPath, commitRev);
        if (fileItem == null) {
            throw new NotFoundException("project.merge.conflict.file.revision.not.found", path, commitRev);
        }
        return fileItem;
    }

    private String getRealPath(Repository repository, String path) throws IOException {
        if (repository.supports().mappedFolders()) {
            return ((FolderMapper) repository).getRealPath(path);
        }
        return path;
    }

    private Repository getRepository(MergeConflictInfo mergeConflict) throws IOException {
        var project = mergeConflict.project();
        if (!mergeConflict.isMerging()) {
            return project.getDesignRepository();
        } else {
            String id = mergeConflict.getRepositoryId();
            return ((BranchRepository) getUserWorkspace().getDesignTimeRepository().getRepository(id))
                    .forBranch(mergeConflict.mergeBranchTo());
        }
    }

    @Override
    public ResolveConflictsResponse resolveConflicts(MergeConflictInfo mergeConflictInfo,
                                                     List<FileConflictResolution> resolutions,
                                                     Map<String, MultipartFile> customFiles,
                                                     String mergeMessage) throws IOException, ProjectException {
        // Validate input
        validateResolutions(mergeConflictInfo, resolutions, customFiles);

        var project = mergeConflictInfo.project();
        boolean isMerging = mergeConflictInfo.isMerging();
        String repositoryId = mergeConflictInfo.getRepositoryId();
        var conflictDetails = mergeConflictInfo.details();

        List<FileItem> resolvedFiles = new ArrayList<>();

        try {
            var workspace = getUserWorkspace();
            var designRepository = workspace.getDesignTimeRepository().getRepository(repositoryId);
            var localRepository = workspace.getLocalWorkspace().getRepository(repositoryId);

            // Prepare resolved files based on strategies
            for (FileConflictResolution resolution : resolutions) {
                String filePath = resolution.filePath();
                FileItem file;
                InputStream stream;

                switch (resolution.strategy()) {
                    case BASE:
                        file = designRepository.readHistory(filePath, conflictDetails.baseCommit());
                        stream = file == null ? null : file.getStream();
                        resolvedFiles.add(new FileItem(filePath, stream));
                        break;

                    case OURS:
                        String oursCommit = mergeConflictInfo.isExportOperation()
                                ? conflictDetails.theirCommit()
                                : conflictDetails.yourCommit();
                        if (isMerging) {
                            file = designRepository.readHistory(filePath, oursCommit);
                        } else {
                            // Read from local workspace
                            Optional<RulesProject> projectByPath = workspace.getProjectByPath(repositoryId, filePath);
                            if (projectByPath.isPresent()) {
                                RulesProject p = projectByPath.get();
                                String artefactPath = filePath.substring(p.getRealPath().length() + 1);
                                String localName = p.getFolderPath() + "/" + artefactPath;
                                file = localRepository.read(localName);
                            } else {
                                file = null;
                            }
                        }
                        stream = file == null ? null : file.getStream();
                        resolvedFiles.add(new FileItem(filePath, stream));
                        break;

                    case THEIRS:
                        String theirsCommit = mergeConflictInfo.isExportOperation()
                                ? conflictDetails.yourCommit()
                                : conflictDetails.theirCommit();
                        file = designRepository.readHistory(filePath, theirsCommit);
                        stream = file == null ? null : file.getStream();
                        resolvedFiles.add(new FileItem(filePath, stream));
                        break;

                    case CUSTOM:
                        MultipartFile uploadedFile = customFiles.get(filePath);
                        resolvedFiles.add(new FileItem(filePath, uploadedFile.getInputStream()));
                        break;
                }
            }

            // Auto-resolve Excel files
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (var autoResolveEntry : conflictDetails.toAutoResolve().entrySet()) {
                String fileName = autoResolveEntry.getKey();
                WorkbookDiffResult diffResult = autoResolveEntry.getValue();

                FileItem yoursConflictedFile;
                String oursCommit = mergeConflictInfo.isExportOperation()
                        ? conflictDetails.theirCommit()
                        : conflictDetails.yourCommit();

                if (isMerging) {
                    yoursConflictedFile = designRepository.readHistory(fileName, oursCommit);
                } else {
                    Optional<RulesProject> projectByPath = workspace.getProjectByPath(repositoryId, fileName);
                    if (projectByPath.isPresent()) {
                        RulesProject p = projectByPath.get();
                        String artefactPath = fileName.substring(p.getRealPath().length() + 1);
                        String localName = p.getFolderPath() + "/" + artefactPath;
                        yoursConflictedFile = localRepository.read(localName);
                    } else {
                        throw new IllegalStateException("Cannot automatically resolve file conflict: " + fileName);
                    }
                }

                String theirsCommit = mergeConflictInfo.isExportOperation()
                        ? conflictDetails.yourCommit()
                        : conflictDetails.theirCommit();
                FileItem theirConflictedFile = designRepository.readHistory(fileName, theirsCommit);

                XlsWorkbookMerger.merge(yoursConflictedFile.getStream(),
                        theirConflictedFile.getStream(),
                        diffResult,
                        output);
                resolvedFiles.add(new FileItem(fileName, new ByteArrayInputStream(output.toByteArray())));
                output.reset();
            }

            // Find modules to append to rules.xml
            Map<String, List<Module>> modulesToAppend = findModulesToAppend(mergeConflictInfo, resolvedFiles);

            // Create conflict resolve data and save
            ConflictResolveData conflictResolveData = new ConflictResolveData(
                    conflictDetails.theirCommit(),
                    resolvedFiles,
                    mergeMessage
            );

            if (isMerging) {
                ((BranchRepository) designRepository).forBranch(mergeConflictInfo.mergeBranchTo())
                        .merge(mergeConflictInfo.mergeBranchFrom(),
                                workspace.getUser().getUserInfo(),
                                conflictResolveData);
            } else {
                project.save(conflictResolveData);
            }

            String branch = isMerging ? mergeConflictInfo.mergeBranchTo() : project.getBranch();
            updateRulesXmlFiles(repositoryId, modulesToAppend, branch, mergeMessage);

            // Return success
            List<String> resolvedFilePaths = resolutions.stream()
                    .map(FileConflictResolution::filePath)
                    .toList();

            return new ResolveConflictsResponse(
                    ConflictResolutionStatus.SUCCESS,
                    resolvedFilePaths);
        } catch (JAXBException e) {
            log.error("Failed to resolve conflicts", e);
            throw new ProjectException("Failed to resolve conflicts: " + e.getMessage(), e);
        } finally {
            for (FileItem file : resolvedFiles) {
                IOUtils.closeQuietly(file.getStream());
            }
        }
    }

    private void validateResolutions(MergeConflictInfo mergeConflictInfo,
                                     List<FileConflictResolution> resolutions,
                                     Map<String, MultipartFile> customFiles) {
        var conflictDetails = mergeConflictInfo.details();
        var conflictedFiles = conflictDetails.getConflictedFiles();

        // Check for duplicates
        Set<String> seenPaths = new HashSet<>();
        for (FileConflictResolution resolution : resolutions) {
            String filePath = resolution.filePath();
            if (!seenPaths.add(filePath)) {
                throw new BadRequestException("project.merge.conflict.duplicate.resolution", new Object[]{filePath});
            }
        }

        // Validate each resolution
        for (FileConflictResolution resolution : resolutions) {
            String filePath = resolution.filePath();

            // Check if file is in conflicted files
            if (!conflictedFiles.contains(filePath)) {
                throw new BadRequestException("project.merge.conflict.file.not.in.conflicts", new Object[]{filePath});
            }

            // Check custom file is provided for CUSTOM strategy
            if (resolution.strategy() == ConflictResolutionStrategy.CUSTOM) {
                if (customFiles == null || !customFiles.containsKey(filePath)) {
                    throw new BadRequestException("project.merge.conflict.custom.file.missing", new Object[]{filePath});
                }
            }
        }
    }

    private Map<String, List<Module>> findModulesToAppend(MergeConflictInfo mergeConflictInfo,
                                                          List<FileItem> resolvedFiles) throws IOException, JAXBException {
        UserWorkspace workspace = getUserWorkspace();
        String repositoryId = mergeConflictInfo.getRepositoryId();
        var conflictDetails = mergeConflictInfo.details();
        Map<String, List<Module>> modulesToAppend = new HashMap<>();

        for (FileItem resolvedFile : resolvedFiles) {
            String name = resolvedFile.getData().getName();
            if (!FileTypeHelper.isExcelFile(name)) {
                continue;
            }

            if (resolvedFile.getStream() != null) {
                Optional<RulesProject> projectByPath = workspace.getProjectByPath(repositoryId, name);
                if (projectByPath.isEmpty()) {
                    continue;
                }

                RulesProject project = projectByPath.get();
                String projectPath = project.getRealPath();
                String rulesXmlFile = projectPath + "/rules.xml";

                String moduleInternalPath = name.substring(projectPath.length() + 1);
                Repository repository = workspace.getDesignTimeRepository().getRepository(repositoryId);

                Module module = null;

                // Try to get module from their commit
                try (FileItem fileItem = repository.readHistory(rulesXmlFile, conflictDetails.theirCommit())) {
                    if (fileItem != null) {
                        module = getModule(fileItem, moduleInternalPath);
                    }
                }

                // If not found, try our commit
                if (module == null) {
                    String oursCommit = mergeConflictInfo.isExportOperation()
                            ? conflictDetails.theirCommit()
                            : conflictDetails.yourCommit();
                    try (FileItem fileItem = repository.readHistory(rulesXmlFile, oursCommit)) {
                        if (fileItem != null) {
                            module = getModule(fileItem, moduleInternalPath);
                        }
                    }
                }

                if (module != null) {
                    List<Module> modules = modulesToAppend.computeIfAbsent(projectPath, k -> new ArrayList<>());
                    modules.add(module);
                }
            }
        }

        return modulesToAppend;
    }

    private void updateRulesXmlFiles(String repositoryId,
                                     Map<String, List<Module>> modulesToAppend,
                                     String branch,
                                     String mergeMessage) throws IOException, JAXBException {
        if (modulesToAppend.isEmpty()) {
            return;
        }

        Repository repository = getUserWorkspace().getDesignTimeRepository().getRepository(repositoryId);
        List<FileItem> files = new ArrayList<>();

        for (Map.Entry<String, List<Module>> entry : modulesToAppend.entrySet()) {
            String projectPath = entry.getKey();
            String rulesXmlFile = projectPath + "/rules.xml";

            try (FileItem fileItem = repository.read(rulesXmlFile)) {
                if (fileItem != null) {
                    ProjectDescriptor descriptor = PROJECT_DESCRIPTOR_SERIALIZER.deserialize(fileItem.getStream());
                    Map<String, Module> modules = new LinkedHashMap<>();

                    // Add existing modules
                    modules.putAll(descriptor.getModules()
                            .stream()
                            .collect(Collectors.toMap(m -> m.getRulesRootPath().getPath(), m -> m)));

                    // Add new modules
                    for (Module module : entry.getValue()) {
                        String path = module.getRulesRootPath().getPath();
                        if (!modules.containsKey(path)) {
                            modules.put(path, module);
                        }
                    }

                    descriptor.setModules(new ArrayList<>(modules.values()));
                    files.add(new FileItem(rulesXmlFile,
                            IOUtils.toInputStream(PROJECT_DESCRIPTOR_SERIALIZER.serialize(descriptor))));
                }
            }
        }

        if (!files.isEmpty()) {
            FileData folderData = new FileData();
            folderData.setName("");
            folderData.setAuthor(getUserWorkspace().getUser().getUserInfo());
            folderData.setComment(mergeMessage);
            folderData.setBranch(branch);
            repository.save(folderData, files, ChangesetType.DIFF);
        }
    }

    private Module getModule(FileItem fileItem, String moduleInternalPath) throws IOException, JAXBException {
        try (InputStream stream = fileItem.getStream()) {
            ProjectDescriptor descriptor = PROJECT_DESCRIPTOR_SERIALIZER.deserialize(stream);
            for (Module module : descriptor.getModules()) {
                if (module.getRulesRootPath().getPath().equals(moduleInternalPath)) {
                    return module;
                }
            }
        }
        return null;
    }
}
