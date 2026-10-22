package org.openl.studio.projects.service.merge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EmptyFileException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.UnsupportedFileFormatException;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.util.RecordFormatException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.MergeConflictDetails;
import org.openl.rules.repository.git.text.TextMerge;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.xls.merge.LinkedWorkbookMismatchException;
import org.openl.rules.xls.merge.XlsWorkbookMerger;
import org.openl.rules.xls.merge.diff.CellResolutionException;
import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.SheetConflictReason;
import org.openl.rules.xls.merge.diff.UnresolvedConflictsException;
import org.openl.rules.xls.merge.diff.WorkbookConflicts;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;
import org.openl.studio.audit.service.ProjectAuditor;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.validation.FileIntegrityValidator;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictCellsDecision;
import org.openl.studio.projects.model.merge.ConflictDetailsResponse;
import org.openl.studio.projects.model.merge.ConflictDetailsResponse.ConflictFileAvailability;
import org.openl.studio.projects.model.merge.ConflictDetailsResponse.RevisionDetails;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.ConflictResolutionStatus;
import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;
import org.openl.studio.projects.model.merge.ConflictTextView;
import org.openl.studio.projects.model.merge.ConflictWorkbookView;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;
import org.openl.studio.projects.service.ProjectIndex;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectsMergeConflictsServiceImpl implements ProjectsMergeConflictsService {

    private final MergeChangesService mergeChangesService;
    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private final ConflictWorkbookMapper workbookMapper;
    private final MergedWorkbookFormatting mergedWorkbookFormatting;
    private final ProjectAuditor auditor;

    @Lookup
    public UserWorkspace getUserWorkspace() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    @Lookup
    public WebStudio getWebStudio() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    @Override
    public ConflictDetailsResponse getConflictDetails(MergeConflictInfo mergeConflictInfo) throws IOException {
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
        var oursCommit = commitOf(mergeConflictInfo, ConflictBase.OURS);
        var theirsCommit = commitOf(mergeConflictInfo, ConflictBase.THEIRS);
        var baseCommit = commitOf(mergeConflictInfo, ConflictBase.BASE);

        // Get branch names
        var oursBranch = getYourBranch(mergeConflictInfo);
        var theirsBranch = getTheirBranch(mergeConflictInfo);

        var conflictedFiles = conflictDetails.getConflictedFiles();
        var oursFiles = getRevisionFiles(rawRepository, oursCommit, conflictedFiles);
        var theirsFiles = getRevisionFiles(rawRepository, theirsCommit, conflictedFiles);
        var baseFiles = getRevisionFiles(rawRepository, baseCommit, conflictedFiles);

        builder.oursRevision(getRevisionDetails(getFirstFile(oursFiles, conflictedFiles), oursCommit, oursBranch))
                .theirsRevision(getRevisionDetails(getFirstFile(theirsFiles, conflictedFiles),
                        theirsCommit,
                        theirsBranch))
                .baseRevision(getRevisionDetails(getFirstFile(baseFiles, conflictedFiles), baseCommit, null));

        var fileAvailability = new LinkedHashMap<String, ConflictFileAvailability>();
        for (var file : conflictDetails.getConflictedFiles()) {
            fileAvailability.put(file, new ConflictFileAvailability(oursFiles.containsKey(file),
                    theirsFiles.containsKey(file),
                    baseFiles.containsKey(file),
                    isText(mergeConflictInfo, file)));
        }
        builder.fileAvailability(fileAvailability);

        // Generate default merge message
        var unresolvedFiles = mergeConflictInfo.details().getConflictedFiles().stream()
                .map(file -> new FileConflictResolution(file, null))
                .toList();
        builder.defaultMessage(generateMergeMessage(mergeConflictInfo, unresolvedFiles));

        return builder.build();
    }

    private Map<String, FileData> getRevisionFiles(Repository repository,
                                                   @Nullable String commit,
                                                   Collection<String> conflictedFiles) throws IOException {
        if (commit == null || conflictedFiles.isEmpty()) {
            return Map.of();
        }

        var files = new HashMap<String, FileData>();
        for (var path : conflictedFiles) {
            var file = repository.checkHistory(path, commit);
            if (file != null) {
                files.put(path, file);
            }
        }
        return files;
    }

    private @Nullable FileData getFirstFile(Map<String, FileData> files, Iterable<String> conflictedFiles) {
        for (var path : conflictedFiles) {
            var file = files.get(path);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private RevisionDetails getRevisionDetails(@Nullable FileData fileData,
                                               @Nullable String commit,
                                               @Nullable String branch) {
        if (commit == null || fileData == null) {
            return RevisionDetails.notExists(commit, branch);
        }

        var author = Optional.ofNullable(fileData.getAuthor())
                .map(UserInfo::getName)
                .orElse(null);
        var modifiedAt = Optional.ofNullable(fileData.getModifiedAt())
                .map(Date::toInstant)
                .orElse(null);
        return RevisionDetails.of(commit, branch, author, modifiedAt);
    }

    @Override
    public List<ConflictGroup> getMergeConflicts(MergeConflictInfo mergeConflictInfo) {
        if (mergeConflictInfo.details().getConflictedFiles().isEmpty()) {
            return List.of();
        }
        var conflicts = new ArrayList<String>(mergeConflictInfo.details().getConflictedFiles());
        var groups = new TreeMap<String, ConflictGroup>((p1, p2) -> {
            if (p1.equals(p2)) {
                return 0;
            }
            // Put empty project name to be latest.
            if (p1.isEmpty()) {
                return 1;
            }
            if (p2.isEmpty()) {
                return -1;
            }
            return p1.compareToIgnoreCase(p2);
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
        return requireConflictVersion(mergeConflict, getRepository(mergeConflict), path, side);
    }

    @Override
    public ConflictTextView getConflictText(MergeConflictInfo mergeConflict, String path) throws IOException {
        var repository = getRepository(mergeConflict);
        var ours = bytesOf(requireConflictVersion(mergeConflict, repository, path, ConflictBase.OURS));
        var theirs = bytesOf(requireConflictVersion(mergeConflict, repository, path, ConflictBase.THEIRS));
        var baseVersion = readConflictVersion(mergeConflict, repository, path, ConflictBase.BASE);
        // The branches that both added the file merge it as added to an empty file.
        var base = baseVersion == null ? new byte[0] : bytesOf(baseVersion);
        return TextMerge.merge(base, ours, theirs)
                .map(ConflictTextView::of)
                .orElseThrow(() -> new BadRequestException("project.merge.conflict.text.not-text.message",
                        new Object[]{path}));
    }

    @Override
    public ConflictWorkbookView getConflictWorkbook(MergeConflictInfo mergeConflict, String path) throws IOException {
        var versions = workbookVersions(mergeConflict, path);
        try (var merger = open(path, versions)) {
            return workbookMapper.toView(merger.getConflicts(), merger);
        }
    }

    @Override
    public byte[] mergeConflictWorkbook(MergeConflictInfo mergeConflict,
                                        String path,
                                        Map<String, ConflictBase> decisions,
                                        Map<String, ConflictCellsDecision> byCells) throws IOException {
        if (decisions.get(WorkbookConflicts.PALETTE_ID) == ConflictBase.BASE) {
            throw new BadRequestException("project.merge.conflict.workbook.palette.message");
        }
        var cellResolutions = CellDecisions.resolutionsOf(path, byCells);
        var versions = workbookVersions(mergeConflict, path);
        var resolutions = new HashMap<String, Resolution>();
        decisions.forEach((id, side) -> resolutions.put(id, switch (side) {
            case BASE -> Resolution.BASE;
            case OURS -> Resolution.OURS;
            case THEIRS -> Resolution.THEIRS;
        }));
        WorkbookDiffResult diff;
        try (var merger = open(path, versions)) {
            diff = merger.resolve(resolutions, cellResolutions);
        } catch (UnresolvedConflictsException e) {
            throw refusalOf(path, e);
        } catch (CellResolutionException e) {
            throw CellDecisions.refusalOf(path, e);
        }
        var output = new ByteArrayOutputStream();
        try {
            XlsWorkbookMerger.merge(new ByteArrayInputStream(versions.base()),
                    new ByteArrayInputStream(versions.ours()),
                    new ByteArrayInputStream(versions.theirs()),
                    diff,
                    output);
        } catch (LinkedWorkbookMismatchException e) {
            throw new BadRequestException("project.merge.conflict.workbook.link.message",
                    new Object[]{e.getSheet() + "!" + e.getCell()});
        }
        var merged = output.toByteArray();
        return formatted(mergeConflict, Map.of(path, merged)).getOrDefault(path, merged);
    }

    /**
     * The workbooks a merge combined, formatted as the merge formats them. Workbooks that cannot be staged to be
     * formatted, as when the branch the merge changes cannot be read, are left as merged: formatting never fails a
     * merge.
     */
    private Map<String, byte[]> formatted(MergeConflictInfo mergeConflict, Map<String, byte[]> workbooks) {
        try {
            return mergedWorkbookFormatting.format(stagedProject(mergeConflict), workbooks, getWebStudio());
        } catch (IOException e) {
            log.warn("The merged workbooks {} are left unformatted.", workbooks.keySet(), e);
            return workbooks;
        }
    }

    /**
     * The project the merged workbooks are staged with to be formatted: the project the resolution is committed to.
     * That is the project as the branch the merge changes holds it, or else the copy in the workspace.
     */
    private static AProjectFolder stagedProject(MergeConflictInfo mergeConflict) throws IOException {
        var project = mergeConflict.project();
        return mergeConflict.isMerging()
                ? MergedWorkbookFormatting.onBranch(project,
                        project.getDesignRepository(),
                        mergeConflict.mergeBranchTo())
                : project;
    }

    /**
     * The versions of a conflicted workbook.
     */
    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    private static final class WorkbookVersions {

        private final byte[] base;
        private final byte[] ours;
        private final byte[] theirs;

        XlsWorkbookMerger open() throws IOException {
            return XlsWorkbookMerger.create(new ByteArrayInputStream(base),
                    new ByteArrayInputStream(ours),
                    new ByteArrayInputStream(theirs));
        }
    }

    /**
     * Opens the versions of a conflicted workbook for its merge.
     *
     * @throws BadRequestException when a version cannot be read as a workbook, such as a damaged or encrypted file
     */
    private static XlsWorkbookMerger open(String path, WorkbookVersions versions) {
        try {
            return versions.open();
        } catch (IOException | UnsupportedFileFormatException | EncryptedDocumentException | EmptyFileException
                 | RecordFormatException | POIXMLException e) {
            log.debug("A version of the conflicted workbook '{}' cannot be read", path, e);
            throw new BadRequestException("project.merge.conflict.workbook.not-workbook.message", new Object[]{path});
        }
    }

    /**
     * Reads every version of a conflicted workbook: a workbook is merged table by table against its base version.
     *
     * @throws BadRequestException when the file is not an Excel workbook
     * @throws NotFoundException   when the file is not conflicted, or a version does not hold it
     */
    private WorkbookVersions workbookVersions(MergeConflictInfo mergeConflict, String path) throws IOException {
        if (!FileTypeHelper.isExcelFile(path)) {
            throw new BadRequestException("project.merge.conflict.workbook.not-workbook.message", new Object[]{path});
        }
        var repository = getRepository(mergeConflict);
        return new WorkbookVersions(bytesOf(requireConflictVersion(mergeConflict, repository, path, ConflictBase.BASE)),
                bytesOf(requireConflictVersion(mergeConflict, repository, path, ConflictBase.OURS)),
                bytesOf(requireConflictVersion(mergeConflict, repository, path, ConflictBase.THEIRS)));
    }

    /**
     * Tells why a workbook cannot be merged as decided: conflicts left without a decision, tables decided cell by cell
     * that cannot be merged so, or else the first sheet whose tables do not fit together as resolved, which leaves the
     * sheet to be taken whole from one version.
     */
    private static BadRequestException refusalOf(String path, UnresolvedConflictsException e) {
        var unresolved = new ArrayList<>(e.getUnresolved());
        e.getRefused().forEach((sheet, reason) -> {
            if (reason != SheetConflictReason.TOUCHING_TABLES && reason != SheetConflictReason.MOVED_CONTENT) {
                unresolved.add(sheet);
            }
        });
        if (!unresolved.isEmpty()) {
            return new BadRequestException("project.merge.conflict.workbook.unresolved.message",
                    new Object[]{path, String.join(", ", unresolved)});
        } else if (!e.getNotByCells().isEmpty()) {
            return new BadRequestException("project.merge.conflict.workbook.not-by-cells.message",
                    new Object[]{path, String.join(", ", e.getNotByCells())});
        }
        var refused = e.getRefused().entrySet().iterator().next();
        return new BadRequestException(refused.getValue() == SheetConflictReason.TOUCHING_TABLES
                ? "project.merge.conflict.workbook.touching.message"
                : "project.merge.conflict.workbook.moved.message", new Object[]{refused.getKey()});
    }

    /**
     * Whether every version of a conflicted file that holds it is UTF-8 text, which a merge of its lines reads. A
     * workbook or an archive is told by its name, without its versions being read.
     *
     * <p>The versions of a conflict do not change, so a file is read for it once and the answer is kept with the
     * conflict. A version that cannot be read makes the file no text until it is asked about again: the failure may
     * pass.
     */
    private boolean isText(MergeConflictInfo mergeConflict, String path) {
        if (FileTypeHelper.isExcelFile(path) || FileTypeHelper.isZipFile(path)) {
            return false;
        }
        var textFiles = conflictsSessionHolder.textFilesOf(mergeConflict);
        var known = textFiles.get(path);
        if (known != null) {
            return known;
        }
        try {
            var repository = getRepository(mergeConflict);
            var text = isText(mergeConflict, repository, path, ConflictBase.OURS)
                    && isText(mergeConflict, repository, path, ConflictBase.THEIRS)
                    && isText(mergeConflict, repository, path, ConflictBase.BASE);
            textFiles.put(path, text);
            return text;
        } catch (IOException e) {
            log.warn("A version of the conflicted file '{}' cannot be read: {}", path, e.getMessage());
            return false;
        }
    }

    /** Whether a version of a conflicted file is text; a version that does not hold the file does not count. */
    private boolean isText(MergeConflictInfo mergeConflict,
                           Repository repository,
                           String path,
                           ConflictBase side) throws IOException {
        var version = readConflictVersion(mergeConflict, repository, path, side);
        if (version == null) {
            return true;
        }
        try (var stream = version.getStream()) {
            return TextMerge.isText(stream);
        }
    }

    /**
     * Reads a version of a conflicted file that the version holds.
     *
     * @throws NotFoundException when the file is not conflicted, or the version does not hold it
     */
    private FileItem requireConflictVersion(MergeConflictInfo mergeConflict,
                                            Repository repository,
                                            String path,
                                            ConflictBase side) throws IOException {
        var fileItem = readConflictVersion(mergeConflict, repository, path, side);
        if (fileItem != null) {
            return fileItem;
        }
        var commit = commitOf(mergeConflict, side);
        // A conflict of histories with no commit in common has no base version.
        throw commit == null
                ? new NotFoundException("project.merge.conflict.file.side.not.found", side, path)
                : new NotFoundException("project.merge.conflict.file.revision.not.found", commit, path);
    }

    /**
     * Reads a version of a conflicted file from the repository the conflict was found in.
     *
     * @return the version, or {@code null} when it does not hold the file
     * @throws NotFoundException when the file is not conflicted
     */
    private @Nullable FileItem readConflictVersion(MergeConflictInfo mergeConflict,
                                                   Repository repository,
                                                   String path,
                                                   ConflictBase side) throws IOException {
        if (!mergeConflict.details().getConflictedFiles().contains(path)) {
            throw new NotFoundException("project.merge.conflict.file.not.found", path);
        }
        var commit = commitOf(mergeConflict, side);
        if (commit == null) {
            return null;
        }
        return repository.readHistory(getRealPath(repository, path), commit);
    }

    /** The commit of a side of the conflict: our and their side are swapped when the current branch is sent. */
    private static @Nullable String commitOf(MergeConflictInfo mergeConflict, ConflictBase side) {
        var conflictDetails = mergeConflict.details();
        return switch (side) {
            case BASE -> conflictDetails.baseCommit();
            case OURS -> mergeConflict.isExportOperation()
                    ? conflictDetails.theirCommit()
                    : conflictDetails.yourCommit();
            case THEIRS -> mergeConflict.isExportOperation()
                    ? conflictDetails.yourCommit()
                    : conflictDetails.theirCommit();
        };
    }

    private static byte[] bytesOf(FileItem fileItem) throws IOException {
        try (var stream = fileItem.getStream()) {
            return stream.readAllBytes();
        }
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
        }
        var id = mergeConflict.getRepositoryId();
        return ((BranchRepository) getUserWorkspace().getDesignTimeRepository().getRepository(id))
                .forBranch(mergeConflict.mergeBranchTo());
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
            var sources = new ConflictSources(mergeConflictInfo,
                    repositoryId,
                    workspace,
                    designRepository,
                    localRepository);

            // Prepare resolved files based on strategies
            for (FileConflictResolution resolution : resolutions) {
                addResolvedFile(sources, resolution, customFiles, resolvedFiles);
            }

            // Auto-resolve Excel files
            addAutoResolvedFiles(sources, resolvedFiles);

            // Find modules to append to rules.xml
            var modulesToAppend = findModulesToAppend(mergeConflictInfo, resolvedFiles);

            // Create conflict resolve data and save
            var conflictResolveData = new ConflictResolveData(
                    conflictDetails.theirCommit(),
                    resolvedFiles,
                    mergeMessage
            );

            String branch = isMerging ? mergeConflictInfo.mergeBranchTo() : project.getBranch();
            @Nullable List<AProject> targetProjectsBefore = null;
            MergeOutcome outcome = null;
            if (isMerging) {
                targetProjectsBefore = mergeChangesService.projectsBefore(repositoryId, branch);
                var branchRepository = (BranchRepository) designRepository;
                outcome = branchRepository.forBranch(branch)
                        .merge(mergeConflictInfo.mergeBranchFrom(),
                                workspace.getUser().getUserInfo(),
                                conflictResolveData);
                auditor.merged(project, mergeConflictInfo.mergeBranchFrom(), branch, true);
                auditor.bypassed(branchRepository, branch, project.getBusinessName());
            } else {
                project.save(conflictResolveData);
                auditor.saved(project, mergeMessage, "conflictResolution");
            }

            var descriptors = updateRulesXmlFiles(mergeConflictInfo, modulesToAppend, branch, mergeMessage);
            // The resolution is committed by now. An index that publishes it late is logged and leaves it
            // resolved: reporting a failure would have the user resolve again what is already committed.
            var published = ProjectIndex.awaitBranch(workspace.getDesignTimeRepository(), designRepository, branch);
            // An index that has not published the merge would tell the projects of the branch as they were before it.
            var changes = outcome == null || !published ? null : mergeChangesService.changesOf(repositoryId,
                    mergeConflictInfo.mergeBranchFrom(),
                    branch,
                    resolvedOutcome(outcome, conflictDetails, descriptors),
                    targetProjectsBefore);

            // Return success
            List<String> resolvedFilePaths = resolutions.stream()
                    .map(FileConflictResolution::filePath)
                    .toList();

            return new ResolveConflictsResponse(ConflictResolutionStatus.SUCCESS,
                    resolvedFilePaths,
                    changes == null ? null : mergeChangesService.summaryOf(project, changes));
        } finally {
            for (FileItem file : resolvedFiles) {
                IOUtils.closeQuietly(file.getStream());
            }
        }
    }

    /**
     * Where the versions of the conflicted files are read from.
     */
    private record ConflictSources(MergeConflictInfo info,
                                   String repositoryId,
                                   UserWorkspace workspace,
                                   Repository designRepository,
                                   LocalRepository localRepository) {
    }

    /**
     * Adds the version of the conflicted file the resolution chooses.
     */
    private static void addResolvedFile(ConflictSources sources,
                                        FileConflictResolution resolution,
                                        Map<String, InputStreamSource> customFiles,
                                        List<FileItem> resolvedFiles) throws IOException {
        var mergeConflictInfo = sources.info();
        var conflictDetails = mergeConflictInfo.details();
        var designRepository = sources.designRepository();
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
                file = readOursFile(sources, filePath);
                stream = file == null ? null : file.getStream();
                resolvedFiles.add(new FileItem(filePath, stream));
                break;

            case THEIRS:
                var theirsCommit = commitOf(mergeConflictInfo, ConflictBase.THEIRS);
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

    /**
     * Reads our version of the conflicted file: the revision of our commit while merging, otherwise the copy in
     * the workspace.
     *
     * @return the file, or {@code null} if no project of the workspace holds it
     */
    private static @Nullable FileItem readOursFile(ConflictSources sources, String filePath) throws IOException {
        var mergeConflictInfo = sources.info();
        FileItem file;
        var oursCommit = commitOf(mergeConflictInfo, ConflictBase.OURS);
        if (mergeConflictInfo.isMerging()) {
            file = sources.designRepository().readHistory(filePath, oursCommit);
        } else {
            // Read from local workspace
            var projectByPath = sources.workspace().getProjectByPath(sources.repositoryId(), filePath);
            if (projectByPath.isPresent()) {
                var p = projectByPath.get();
                var artefactPath = filePath.substring(p.getRealPath().length() + 1);
                var localName = p.getFolderPath() + "/" + artefactPath;
                file = sources.localRepository().read(localName);
            } else {
                file = null;
            }
        }
        return file;
    }

    /**
     * Adds the Excel files whose conflicts are resolved automatically, merged from our and their version and formatted
     * as a merge formats the workbooks it combines.
     */
    private void addAutoResolvedFiles(ConflictSources sources, List<FileItem> resolvedFiles) throws IOException {
        var mergeConflictInfo = sources.info();
        var conflictDetails = mergeConflictInfo.details();
        var designRepository = sources.designRepository();
        var output = new ByteArrayOutputStream();
        var merged = new LinkedHashMap<String, byte[]>();
        for (var autoResolveEntry : conflictDetails.toAutoResolve().entrySet()) {
            var fileName = autoResolveEntry.getKey();
            var diffResult = autoResolveEntry.getValue();

            FileItem yoursConflictedFile;
            var oursCommit = commitOf(mergeConflictInfo, ConflictBase.OURS);

            if (mergeConflictInfo.isMerging()) {
                yoursConflictedFile = designRepository.readHistory(fileName, oursCommit);
            } else {
                var projectByPath = sources.workspace().getProjectByPath(sources.repositoryId(), fileName);
                if (projectByPath.isPresent()) {
                    var p = projectByPath.get();
                    var artefactPath = fileName.substring(p.getRealPath().length() + 1);
                    var localName = p.getFolderPath() + "/" + artefactPath;
                    yoursConflictedFile = sources.localRepository().read(localName);
                } else {
                    throw new IllegalStateException("Cannot automatically resolve file conflict: " + fileName);
                }
            }

            var theirsCommit = commitOf(mergeConflictInfo, ConflictBase.THEIRS);
            var theirConflictedFile = designRepository.readHistory(fileName, theirsCommit);

            XlsWorkbookMerger.merge(yoursConflictedFile.getStream(),
                    theirConflictedFile.getStream(),
                    diffResult,
                    output);
            merged.put(fileName, output.toByteArray());
            output.reset();
        }
        if (!merged.isEmpty()) {
            var workbooks = formatted(mergeConflictInfo, merged);
            merged.forEach((path, workbook) -> resolvedFiles.add(
                    new FileItem(path, new ByteArrayInputStream(workbooks.getOrDefault(path, workbook)))));
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
                if (!customFiles.containsKey(filePath)) {
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
        var modulesToAppend = new HashMap<String, List<Module>>();

        for (FileItem resolvedFile : resolvedFiles) {
            var name = resolvedFile.getData().getName();
            if (!FileTypeHelper.isExcelFile(name) || resolvedFile.getStream() == null) {
                continue;
            }

            var projectByPath = workspace.getProjectByPath(repositoryId, name);
            if (projectByPath.isPresent()) {
                var project = projectByPath.get();
                var projectPath = project.getRealPath();
                var rulesXmlFile = projectPath + "/rules.xml";

                var moduleInternalPath = name.substring(projectPath.length() + 1);
                var repository = workspace.getDesignTimeRepository().getRepository(repositoryId);

                var module = findResolvedModule(mergeConflictInfo, repository, rulesXmlFile, moduleInternalPath);

                if (module != null) {
                    var modules = modulesToAppend.computeIfAbsent(projectPath, k -> new ArrayList<>());
                    modules.add(module);
                }
            }
        }

        return modulesToAppend;
    }

    /**
     * The module of the resolved file as rules.xml of their commit declares it, or else as the one of our commit
     * does.
     *
     * @return the module, or {@code null} if neither commit declares it
     */
    private @Nullable Module findResolvedModule(MergeConflictInfo mergeConflictInfo,
                                                Repository repository,
                                                String rulesXmlFile,
                                                String moduleInternalPath) throws IOException {
        var conflictDetails = mergeConflictInfo.details();
        Module module = null;

        // Try to get module from their commit
        try (var fileItem = repository.readHistory(rulesXmlFile, conflictDetails.theirCommit())) {
            if (fileItem != null) {
                module = getModule(fileItem, moduleInternalPath);
            }
        }

        // If not found, try our commit
        if (module == null) {
            var oursCommit = commitOf(mergeConflictInfo, ConflictBase.OURS);
            try (var fileItem = repository.readHistory(rulesXmlFile, oursCommit)) {
                if (fileItem != null) {
                    module = getModule(fileItem, moduleInternalPath);
                }
            }
        }
        return module;
    }

    /**
     * The merge as the resolution completed it: with the workbooks the resolution merged automatically, and with the
     * descriptors written after its commit, which count among the files it changed. The commit of the descriptors
     * stands for the merge, so the version after it holds them.
     */
    private static MergeOutcome resolvedOutcome(MergeOutcome outcome,
                                                MergeConflictDetails conflictDetails,
                                                @Nullable SavedDescriptors descriptors) {
        var workbooks = MergeConflictDetails.mergedWorkbooks(conflictDetails.toAutoResolve());
        if (descriptors == null) {
            return new MergeOutcome(outcome.before(), outcome.after(), outcome.changes(), workbooks);
        }
        var listed = outcome.changes().stream().map(FileChange::path).collect(Collectors.toSet());
        var changes = new ArrayList<>(outcome.changes());
        descriptors.paths()
                .stream()
                .filter(path -> !listed.contains(path))
                .forEach(path -> changes.add(new FileChange(path, FileChange.Kind.MODIFIED)));
        return new MergeOutcome(outcome.before(), descriptors.revision(), changes, workbooks);
    }

    /**
     * The descriptors written in a commit of their own after a resolution.
     *
     * @param revision the revision of that commit
     * @param paths    the paths of the descriptors from the root of the repository
     */
    private record SavedDescriptors(String revision, List<String> paths) {
    }

    /**
     * Declares the modules of the resolved workbooks again in the descriptors of their projects, in a commit of
     * its own on the branch the resolution was committed to.
     *
     * @return the descriptors written, or {@code null} when none needed it or the save told no revision of them
     */
    private @Nullable SavedDescriptors updateRulesXmlFiles(MergeConflictInfo mergeConflictInfo,
                                                           Map<String, List<Module>> modulesToAppend,
                                                           String branch,
                                                           String mergeMessage) throws IOException {
        if (modulesToAppend.isEmpty()) {
            return null;
        }

        var repository = getRepository(mergeConflictInfo);
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
                    var added = false;
                    for (Module module : entry.getValue()) {
                        added |= modules.putIfAbsent(module.getRulesRootPath(), module) == null;
                    }

                    // A descriptor declaring every module already is left as it is: rewriting it changes nothing.
                    if (added) {
                        descriptor.setModules(new ArrayList<>(modules.values()));
                        files.add(new FileItem(rulesXmlFile,
                                new ByteArrayInputStream(descriptor.toBytes())));
                    }
                }
            }
        }

        if (!files.isEmpty()) {
            var folderData = new FileData();
            folderData.setName("");
            folderData.setAuthor(getUserWorkspace().getUser().getUserInfo());
            folderData.setComment(mergeMessage);
            folderData.setBranch(branch);
            var saved = repository.save(folderData, files, ChangesetType.DIFF);
            // Without the revision of their commit the descriptors cannot be compared after the merge.
            if (saved != null && saved.getVersion() != null) {
                return new SavedDescriptors(saved.getVersion(),
                        files.stream().map(FileItem::getData).map(FileData::getName).toList());
            }
        }
        return null;
    }

    private @Nullable Module getModule(FileItem fileItem, String moduleInternalPath) throws IOException {
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

            var file = messagePath(resolution.filePath(), designRepository, rulesLocation);
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
                var file = messagePath(entry.getKey(), designRepository, rulesLocation);
                messageBuilder.append("\n\t").append(file);
                var diffResult = entry.getValue();
                var sheetDiffResult = diffResult.getSheetDiffResult();
                appendSheets(messageBuilder, sheetDiffResult.getDiffSheets(DiffStatus.OUR), yourBranch);
                appendSheets(messageBuilder, sheetDiffResult.getDiffSheets(DiffStatus.THEIR), theirBranch);
                appendSheets(messageBuilder,
                        sheetDiffResult.getDiffSheets(DiffStatus.MERGED),
                        merging ? yourBranch + ", " + theirBranch : null);
            }
        }
        return messageBuilder.toString();
    }

    /**
     * The path of the file as a merge message shows it.
     */
    private static String messagePath(String file, Repository designRepository, String rulesLocation) {
        // In non-flat repositories we should see full path. In flat repos only essential part.
        if (!designRepository.supports().mappedFolders() && file.startsWith(rulesLocation)) {
            return file.substring(rulesLocation.length());
        }
        return file;
    }

    /**
     * Appends the names of the sheets, each with the branch its version comes from when it is known.
     */
    private static void appendSheets(StringBuilder messageBuilder,
                                     Collection<String> sheetNames,
                                     @Nullable String branch) {
        for (String sheetName : sheetNames) {
            messageBuilder.append("\n\t\t").append(sheetName);
            if (branch != null) {
                messageBuilder.append(" (").append(branch).append(')');
            }
        }
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
