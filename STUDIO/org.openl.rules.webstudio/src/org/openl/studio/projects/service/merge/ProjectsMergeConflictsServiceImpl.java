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
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.xls.merge.XlsWorkbookMerger;
import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.validation.FileIntegrityValidator;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictDetailsResponse;
import org.openl.studio.projects.model.merge.ConflictDetailsResponse.RevisionDetails;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.ConflictResolutionStatus;
import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

@Service
@Slf4j
public class ProjectsMergeConflictsServiceImpl implements ProjectsMergeConflictsService {

    private static final long PROJECT_INDEX_TIMEOUT_SECONDS = 30;

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Override
    public ConflictDetailsResponse getConflictDetails(MergeConflictInfo mergeConflictInfo) {
        var builder = ConflictDetailsResponse.builder()
                .conflictGroups(getMergeConflicts(mergeConflictInfo));
        var conflictDetails = mergeConflictInfo.details();

        // Get repository
        var repositoryId = mergeConflictInfo.getRepositoryId();
        var workspace = getUserWorkspace();
        var designRepository = workspace.getDesignTimeRepository().getRepository(repositoryId);

        // Unwrap FolderMapper if needed
        var rawRepository = designRepository;
        if (designRepository.supports().mappedFolders()) {
            rawRepository = ((FolderMapper) designRepository).getDelegate();
        }

        // Get commit details for each side
        String oursCommit = mergeConflictInfo.isExportOperation()
                ? conflictDetails.theirCommit()
                : conflictDetails.yourCommit();
        String theirsCommit = mergeConflictInfo.isExportOperation()
                ? conflictDetails.yourCommit()
                : conflictDetails.theirCommit();
        var baseCommit = conflictDetails.baseCommit();

        // Get branch names
        var oursBranch = getYourBranch(mergeConflictInfo);
        var theirsBranch = getTheirBranch(mergeConflictInfo);

        // Find first conflicted file to get revision details
        String firstFile = conflictDetails.getConflictedFiles().isEmpty()
                ? null
                : conflictDetails.getConflictedFiles().iterator().next();

        builder.oursRevision(getRevisionDetails(rawRepository, firstFile, oursCommit, oursBranch))
                .theirsRevision(getRevisionDetails(rawRepository, firstFile, theirsCommit, theirsBranch))
                .baseRevision(getRevisionDetails(rawRepository, firstFile, baseCommit, null));

        // Generate default merge message
        var unresolvedFiles = mergeConflictInfo.details().getConflictedFiles().stream()
                .map(file -> new FileConflictResolution(file, null))
                .toList();
        builder.defaultMessage(generateMergeMessage(mergeConflictInfo, unresolvedFiles));

        return builder.build();
    }

    private RevisionDetails getRevisionDetails(Repository repository, String file, String commit, String branch) {
        if (commit == null || file == null) {
            return RevisionDetails.notExists(commit, branch);
        }

        try {
            var fileData = repository.checkHistory(file, commit);
            if (fileData != null) {
                var author = Optional.ofNullable(fileData.getAuthor())
                        .map(UserInfo::getName)
                        .orElse(null);
                var modifiedAt = Optional.ofNullable(fileData.getModifiedAt())
                        .map(java.util.Date::toInstant)
                        .orElse(null);
                return RevisionDetails.of(commit, branch, author, modifiedAt);
            }
        } catch (IOException e) {
            log.debug("Failed to get revision details for file {} at commit {}", file, commit, e);
        }

        return RevisionDetails.notExists(commit, branch);
    }

    @Override
    public List<ConflictGroup> getMergeConflicts(MergeConflictInfo mergeConflictInfo) {
        if (mergeConflictInfo.details() == null || mergeConflictInfo.details().getConflictedFiles().isEmpty()) {
            return List.of();
        }
        var conflicts = new ArrayList<String>(mergeConflictInfo.details().getConflictedFiles());
        var groups = new TreeMap<String, ConflictGroup>((p1, p2) -> {
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
        var repositoryId = mergeConflictInfo.getRepositoryId();
        for (String conflict : conflicts) {
            var projectByPath = workspace.getProjectByPath(repositoryId, conflict);
            String projectName;
            String projectPath;
            if (projectByPath.isPresent()) {
                var project = projectByPath.get();
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

    private String getRealPath(Repository repository, String path) {
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
            var id = mergeConflict.getRepositoryId();
            return ((BranchRepository) getUserWorkspace().getDesignTimeRepository().getRepository(id))
                    .forBranch(mergeConflict.mergeBranchTo());
        }
    }

    @Override
    public ResolveConflictsResponse resolveConflicts(MergeConflictInfo mergeConflictInfo,
                                                     List<FileConflictResolution> resolutions,
                                                     Map<String, InputStreamSource> customFiles,
                                                     String mergeMessage) throws IOException, ProjectException {
        // Validate input
        validateResolutions(mergeConflictInfo, resolutions, customFiles);

        if (StringUtils.isBlank(mergeMessage)) {
            mergeMessage = generateMergeMessage(mergeConflictInfo, resolutions);
        }

        var project = mergeConflictInfo.project();
        var isMerging = mergeConflictInfo.isMerging();
        var repositoryId = mergeConflictInfo.getRepositoryId();
        var conflictDetails = mergeConflictInfo.details();

        var resolvedFiles = new ArrayList<FileItem>();

        try {
            var workspace = getUserWorkspace();
            var designRepository = workspace.getDesignTimeRepository().getRepository(repositoryId);
            var localRepository = workspace.getLocalWorkspace().getRepository(repositoryId);

            // Prepare resolved files based on strategies
            for (FileConflictResolution resolution : resolutions) {
                var filePath = resolution.filePath();
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
                            var projectByPath = workspace.getProjectByPath(repositoryId, filePath);
                            if (projectByPath.isPresent()) {
                                var p = projectByPath.get();
                                var artefactPath = filePath.substring(p.getRealPath().length() + 1);
                                var localName = p.getFolderPath() + "/" + artefactPath;
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
                        var uploadedFile = customFiles.get(filePath);
                        resolvedFiles.add(new FileItem(filePath, uploadedFile.getInputStream()));
                        break;
                }
            }

            // Auto-resolve Excel files
            var output = new ByteArrayOutputStream();
            for (var autoResolveEntry : conflictDetails.toAutoResolve().entrySet()) {
                var fileName = autoResolveEntry.getKey();
                var diffResult = autoResolveEntry.getValue();

                FileItem yoursConflictedFile;
                String oursCommit = mergeConflictInfo.isExportOperation()
                        ? conflictDetails.theirCommit()
                        : conflictDetails.yourCommit();

                if (isMerging) {
                    yoursConflictedFile = designRepository.readHistory(fileName, oursCommit);
                } else {
                    var projectByPath = workspace.getProjectByPath(repositoryId, fileName);
                    if (projectByPath.isPresent()) {
                        var p = projectByPath.get();
                        var artefactPath = fileName.substring(p.getRealPath().length() + 1);
                        var localName = p.getFolderPath() + "/" + artefactPath;
                        yoursConflictedFile = localRepository.read(localName);
                    } else {
                        throw new IllegalStateException("Cannot automatically resolve file conflict: " + fileName);
                    }
                }

                String theirsCommit = mergeConflictInfo.isExportOperation()
                        ? conflictDetails.yourCommit()
                        : conflictDetails.theirCommit();
                var theirConflictedFile = designRepository.readHistory(fileName, theirsCommit);

                XlsWorkbookMerger.merge(yoursConflictedFile.getStream(),
                        theirConflictedFile.getStream(),
                        diffResult,
                        output);
                resolvedFiles.add(new FileItem(fileName, new ByteArrayInputStream(output.toByteArray())));
                output.reset();
            }

            // Find modules to append to rules.xml
            var modulesToAppend = findModulesToAppend(mergeConflictInfo, resolvedFiles);

            // Create conflict resolve data and save
            var conflictResolveData = new ConflictResolveData(
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
            awaitProjectIndex(designRepository, repositoryId, branch);

            // Return success
            List<String> resolvedFilePaths = resolutions.stream()
                    .map(FileConflictResolution::filePath)
                    .toList();

            return new ResolveConflictsResponse(
                    ConflictResolutionStatus.SUCCESS,
                    resolvedFilePaths);
        } finally {
            for (FileItem file : resolvedFiles) {
                IOUtils.closeQuietly(file.getStream());
            }
        }
    }

    private void awaitProjectIndex(Repository repository, String repositoryId, String branch) throws IOException {
        if (!repository.supports().branches() || branch == null) {
            return;
        }
        try {
            getUserWorkspace().getDesignTimeRepository()
                    .refreshBranch(repositoryId, branch)
                    .toCompletableFuture()
                    .get(PROJECT_INDEX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while publishing the resolved branch.", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException("The resolved branch was not published by the project index.", e);
        }
    }

    private void validateResolutions(MergeConflictInfo mergeConflictInfo,
                                     List<FileConflictResolution> resolutions,
                                     Map<String, InputStreamSource> customFiles) {
        var conflictDetails = mergeConflictInfo.details();
        var conflictedFiles = conflictDetails.getConflictedFiles();

        // Check for duplicates
        var seenPaths = new HashSet<String>();
        for (FileConflictResolution resolution : resolutions) {
            var filePath = resolution.filePath();
            if (!seenPaths.add(filePath)) {
                throw new BadRequestException("project.merge.conflict.duplicate.resolution", new Object[]{filePath});
            }
        }

        // Validate each resolution
        for (FileConflictResolution resolution : resolutions) {
            var filePath = resolution.filePath();

            // Check if file is in conflicted files
            if (!conflictedFiles.contains(filePath)) {
                throw new BadRequestException("project.merge.conflict.file.not.in.conflicts", new Object[]{filePath});
            }

            // Check custom file is provided for CUSTOM strategy
            if (resolution.strategy() == ConflictResolutionStrategy.CUSTOM) {
                if (customFiles == null || !customFiles.containsKey(filePath)) {
                    throw new BadRequestException("project.merge.conflict.custom.file.missing", new Object[]{filePath});
                }
                verifyIntegrity(filePath, customFiles.get(filePath));
            }
        }
    }

    /**
     * Verifies that an uploaded resolution arrived complete, so a merge is never resolved with a
     * workbook that was cut short on its way here. The upload is read once for the check and read
     * again when the resolution is written.
     */
    private static void verifyIntegrity(String filePath, InputStreamSource customFile) {
        var fileName = FileUtils.getName(filePath);
        if (!FileIntegrityValidator.isVerified(fileName)) {
            return;
        }
        try {
            FileIntegrityValidator.verifyContent(fileName, customFile.getInputStream());
        } catch (IOException e) {
            throw new BadRequestException("project.merge.conflict.custom.file.damaged",
                    new Object[]{filePath, e.getMessage()});
        }
    }

    private Map<String, List<Module>> findModulesToAppend(MergeConflictInfo mergeConflictInfo,
                                                          List<FileItem> resolvedFiles) throws IOException {
        var workspace = getUserWorkspace();
        var repositoryId = mergeConflictInfo.getRepositoryId();
        var conflictDetails = mergeConflictInfo.details();
        var modulesToAppend = new HashMap<String, List<Module>>();

        for (FileItem resolvedFile : resolvedFiles) {
            var name = resolvedFile.getData().getName();
            if (!FileTypeHelper.isExcelFile(name)) {
                continue;
            }

            if (resolvedFile.getStream() != null) {
                var projectByPath = workspace.getProjectByPath(repositoryId, name);
                if (projectByPath.isEmpty()) {
                    continue;
                }

                var project = projectByPath.get();
                var projectPath = project.getRealPath();
                var rulesXmlFile = projectPath + "/rules.xml";

                var moduleInternalPath = name.substring(projectPath.length() + 1);
                var repository = workspace.getDesignTimeRepository().getRepository(repositoryId);

                Module module = null;

                // Try to get module from their commit
                try (var fileItem = repository.readHistory(rulesXmlFile, conflictDetails.theirCommit())) {
                    if (fileItem != null) {
                        module = getModule(fileItem, moduleInternalPath);
                    }
                }

                // If not found, try our commit
                if (module == null) {
                    String oursCommit = mergeConflictInfo.isExportOperation()
                            ? conflictDetails.theirCommit()
                            : conflictDetails.yourCommit();
                    try (var fileItem = repository.readHistory(rulesXmlFile, oursCommit)) {
                        if (fileItem != null) {
                            module = getModule(fileItem, moduleInternalPath);
                        }
                    }
                }

                if (module != null) {
                    var modules = modulesToAppend.computeIfAbsent(projectPath, k -> new ArrayList<>());
                    modules.add(module);
                }
            }
        }

        return modulesToAppend;
    }

    private void updateRulesXmlFiles(String repositoryId,
                                     Map<String, List<Module>> modulesToAppend,
                                     String branch,
                                     String mergeMessage) throws IOException {
        if (modulesToAppend.isEmpty()) {
            return;
        }

        var repository = getUserWorkspace().getDesignTimeRepository().getRepository(repositoryId);
        var files = new ArrayList<FileItem>();

        for (Map.Entry<String, List<Module>> entry : modulesToAppend.entrySet()) {
            var projectPath = entry.getKey();
            var rulesXmlFile = projectPath + "/rules.xml";

            try (var fileItem = repository.read(rulesXmlFile)) {
                if (fileItem != null) {
                    ProjectDescriptor descriptor = ProjectDescriptor.read(fileItem.getStream());
                    var modules = new LinkedHashMap<String, Module>();

                    // Add existing modules
                    modules.putAll(descriptor.getModules()
                            .stream()
                            .collect(Collectors.toMap(Module::getRulesRootPath, m -> m)));

                    // Add new modules
                    for (Module module : entry.getValue()) {
                        var path = module.getRulesRootPath();
                        if (!modules.containsKey(path)) {
                            modules.put(path, module);
                        }
                    }

                    descriptor.setModules(new ArrayList<>(modules.values()));
                    files.add(new FileItem(rulesXmlFile,
                            new ByteArrayInputStream(descriptor.toBytes())));
                }
            }
        }

        if (!files.isEmpty()) {
            var folderData = new FileData();
            folderData.setName("");
            folderData.setAuthor(getUserWorkspace().getUser().getUserInfo());
            folderData.setComment(mergeMessage);
            folderData.setBranch(branch);
            repository.save(folderData, files, ChangesetType.DIFF);
        }
    }

    private Module getModule(FileItem fileItem, String moduleInternalPath) throws IOException {
        try (var stream = fileItem.getStream()) {
            ProjectDescriptor descriptor = ProjectDescriptor.read(stream);
            for (Module module : descriptor.getModules()) {
                if (module.getRulesRootPath().equals(moduleInternalPath)) {
                    return module;
                }
            }
        }
        return null;
    }

    private String generateMergeMessage(MergeConflictInfo mergeConflict, List<FileConflictResolution> resolutions) {
        var conflictDetails = mergeConflict.details();

        var rulesLocation = getUserWorkspace().getDesignTimeRepository().getRulesLocation();

        var messageBuilder = new StringBuilder("Merge with commit " + conflictDetails.theirCommit() + "\nConflicts:");
        var merging = mergeConflict.isMerging();
        var yourBranch = getYourBranch(mergeConflict);
        var theirBranch = getTheirBranch(mergeConflict);
        var designRepository = getUserWorkspace().getDesignTimeRepository()
                .getRepository(mergeConflict.getRepositoryId());
        for (var resolution : resolutions) {

            var file = resolution.filePath();
            // In non-flat repositories we should see full path. In flat repos only essential part.
            if (!designRepository.supports().mappedFolders()) {
                if (file.startsWith(rulesLocation)) {
                    file = file.substring(rulesLocation.length());
                }
            }
            messageBuilder.append("\n\t").append(file);

            var strategy = resolution.strategy();
            if (strategy != null) {
                var chosen = strategy.name().toLowerCase();
                if (merging) {
                    chosen = switch (strategy) {
                        case OURS -> yourBranch;
                        case THEIRS -> theirBranch;
                        default -> chosen;
                    };
                }
                messageBuilder.append(" (").append(chosen).append(')');
            }
        }

        if (!conflictDetails.toAutoResolve().isEmpty()) {
            messageBuilder.append("\n\n Automatically resolved conflicts:");
            for (Map.Entry<String, WorkbookDiffResult> entry : conflictDetails.toAutoResolve().entrySet()) {
                var file = entry.getKey();
                if (!designRepository.supports().mappedFolders()) {
                    if (file.startsWith(rulesLocation)) {
                        file = file.substring(rulesLocation.length());
                    }
                }
                messageBuilder.append("\n\t").append(file);
                var diffResult = entry.getValue();
                var sheetDiffResult = diffResult.getSheetDiffResult();
                for (String sheetName : sheetDiffResult.getDiffSheets(DiffStatus.OUR)) {
                    messageBuilder.append("\n\t\t").append(sheetName);
                    if (yourBranch != null) {
                        messageBuilder.append(" (").append(yourBranch).append(')');
                    }
                }
                for (String sheetName : sheetDiffResult.getDiffSheets(DiffStatus.THEIR)) {
                    messageBuilder.append("\n\t\t").append(sheetName);
                    if (theirBranch != null) {
                        messageBuilder.append(" (").append(theirBranch).append(')');
                    }
                }
            }
        }
        return messageBuilder.toString();
    }

    private String getYourBranch(MergeConflictInfo mergeConflict) {
        return mergeConflict.isExportOperation()
                ? mergeConflict.mergeBranchFrom()
                : mergeConflict.mergeBranchTo();
    }

    private String getTheirBranch(MergeConflictInfo mergeConflict) {
        return mergeConflict.isExportOperation()
                ? mergeConflict.mergeBranchTo()
                : mergeConflict.mergeBranchFrom();
    }
}
