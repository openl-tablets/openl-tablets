package org.openl.rules.repository;

import static org.openl.rules.repository.api.Repository.validatePath;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.BranchTreeRevision;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.MergeScope;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RevisionChanges;
import org.openl.rules.repository.api.SearchableRepository;
import org.openl.rules.repository.api.UserInfo;

/**
 * A wrapper class to check arguments against path traversal vulnerability in the Repository API.
 * It solves the issue only, if the Repository instantiated via {@code org.openl.rules.repository.RepositoryInstatiator}
 *
 * <p>It also keeps the {@link IgnoredPaths ignored paths} out of reach. An ignored file is never listed, and reading it
 * finds nothing. Writing one file to an ignored path is refused with {@link AccessDeniedException}, while a write of
 * several files leaves the ignored ones out. A save of a whole folder keeps the ignored files it holds, and a deletion
 * that names an ignored file deletes nothing. A deleted folder goes with everything in it, its ignored files included,
 * as a deleted project does. The changes of a merge leave the ignored files out.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PathCheckedRepository implements BranchRepository {

    /** The repository whose paths are checked. */
    @Getter
    private final Repository delegate;

    private final IgnoredPaths ignoredPaths;

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        validatePath(path);
        return isIgnored(path) ? List.of() : shown(delegate.list(path), FileData::getName);
    }

    @Override
    public @Nullable FileData check(String name) throws IOException {
        validatePath(name);
        return isIgnored(name) ? null : delegate.check(name);
    }

    @Override
    public @Nullable FileItem read(String name) throws IOException {
        validatePath(name);
        return isIgnored(name) ? null : delegate.read(name);
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        validatePath(data.getName());
        refuseIgnored(data.getName());
        return delegate.save(data, stream);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        fileItems.forEach(x -> validatePath(x.getData().getName()));
        var shown = shown(fileItems, PathCheckedRepository::nameOf);
        return shown.isEmpty() ? List.of() : delegate.save(shown);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        validatePath(data.getName());
        return !isIgnored(data.getName()) && delegate.delete(data);
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        data.forEach(x -> validatePath(x.getName()));
        var shown = shown(data, FileData::getName);
        return !shown.isEmpty() && delegate.delete(shown);
    }

    @Override
    public void setListener(Listener callback) {
        delegate.setListener(callback);
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        validatePath(name);
        return isIgnored(name) ? List.of() : delegate.listHistory(name);
    }

    @Override
    public @Nullable FileData checkHistory(String name, String version) throws IOException {
        validatePath(name);
        return isIgnored(name) ? null : delegate.checkHistory(name, version);
    }

    @Override
    public @Nullable FileItem readHistory(String name, String version) throws IOException {
        validatePath(name);
        return isIgnored(name) ? null : delegate.readHistory(name, version);
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        validatePath(data.getName());
        return !isIgnored(data.getName()) && delegate.deleteHistory(data);
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        validatePath(srcName);
        validatePath(destData.getName());
        if (isIgnored(srcName)) {
            throw new FileNotFoundException("File '%s' is not found.".formatted(srcName));
        }
        refuseIgnored(destData.getName());
        return delegate.copyHistory(srcName, destData, version);
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        validatePath(path);
        return isIgnored(path) ? List.of() : shown(delegate.listFolders(path), FileData::getName);
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        validatePath(path);
        return isIgnored(path) ? List.of() : shown(delegate.listFiles(path, version), FileData::getName);
    }

    // Checking the files first needs a Repository API that reads them twice; checking during the save stops it halfway.
    @SuppressWarnings("java:S1134")
    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException {
        validatePath(folderData.getName());
        refuseIgnored(folderData.getName());
        // FIXME: this Iterable cannot be used multiple times due delegating it to the Stream API.
        //     It needs to refactor the Repository API to allow the following:
        //     files.peek(x -> validatePath(x.getData().getName()))
        if (changesetType == ChangesetType.FULL && !(delegate instanceof IgnoringRepository)) {
            var existing = filesOf(folderData);
            if (existing.stream().anyMatch(fileData -> isIgnored(fileData.getName()))) {
                // A full save deletes every file it is not given, the ignored ones too. So the given files are saved
                // as changes, followed by the deletion of each other file that is not ignored.
                var changes = withAbsentDeleted(files, shown(existing, FileData::getName));
                return delegate.save(folderData, changes, ChangesetType.DIFF);
            }
        }
        return delegate.save(folderData, () -> shownItems(files).iterator(), changesetType);
    }

    /** The files a folder holds now, or in the version a save of the folder starts from. */
    private List<FileData> filesOf(FileData folderData) throws IOException {
        var name = folderData.getName();
        var folder = name.isEmpty() ? name : name + "/";
        var version = folderData.getVersion();
        return version != null && delegate.supports().versions()
                ? delegate.listFiles(folder, version)
                : delegate.list(folder);
    }

    /**
     * The files of a save, followed by the deletion of each existing file the save does not hold.
     *
     * <p>The deletions are known once all the files are read, so the files are still read once, in their order.
     */
    private Iterable<FileItem> withAbsentDeleted(Iterable<FileItem> files, List<FileData> existing) {
        return () -> {
            var saved = new HashSet<String>();
            var changes = shownItems(files).map(item -> {
                saved.add(item.getData().getName());
                return item;
            });
            var deletions = existing.stream()
                    .filter(fileData -> !saved.contains(fileData.getName()))
                    .map(fileData -> new FileItem(fileData.getName(), null));
            return Stream.concat(changes, deletions).iterator();
        };
    }

    private Stream<FileItem> shownItems(Iterable<FileItem> files) {
        return StreamSupport.stream(files.spliterator(), false).filter(item -> !isIgnored(nameOf(item)));
    }

    private static String nameOf(FileItem item) {
        return item.getData().getName();
    }

    /** The items whose paths are not ignored, in a list the caller may change. */
    private <T> List<T> shown(Collection<T> items, Function<T, String> pathOf) {
        return items.stream()
                .filter(item -> !isIgnored(pathOf.apply(item)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean isIgnored(String path) {
        return ignoredPaths.matches(path);
    }

    private void refuseIgnored(String path) throws AccessDeniedException {
        if (isIgnored(path)) {
            throw new AccessDeniedException(path, null, "The path is ignored by the repository.");
        }
    }

    @Override
    public Features supports() {
        return delegate.supports();
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public void validateConnection() throws IOException {
        delegate.validateConnection();
    }

    @Override
    public boolean isMergedInto(String from, String to) throws IOException {
        return ((BranchRepository) delegate).isMergedInto(from, to);
    }

    @Override
    public MergeScope getMergeScope(String from, String to) throws IOException {
        validateBranch(from);
        validateBranch(to);
        var scope = ((BranchRepository) delegate).getMergeScope(from, to);
        return new MergeScope(shown(scope.changes(), FileChange::path),
                Set.copyOf(shown(scope.changedInBoth(), Function.identity())));
    }

    @Override
    public RevisionChanges listChanges(String path, @Nullable String from, String to) throws IOException {
        if (!path.isEmpty()) {
            validatePath(path);
        }
        var changes = ((BranchRepository) delegate).listChanges(path, from, to);
        return new RevisionChanges(changes.from(), changes.to(), shown(changes.changes(), FileChange::path));
    }

    @Override
    public String getBranch() {
        return ((BranchRepository) delegate).getBranch();
    }

    @Override
    public boolean isBranchProtected(String branch) {
        return ((BranchRepository) delegate).isBranchProtected(branch);
    }

    @Override
    public void createRepositoryBranch(String branch, @Nullable String startPoint) throws IOException {
        validateBranch(branch);
        ((BranchRepository) delegate).createRepositoryBranch(branch, startPoint);
    }

    @Override
    public void deleteRepositoryBranch(String branch) throws IOException {
        validateBranch(branch);
        ((BranchRepository) delegate).deleteRepositoryBranch(branch);
    }

    @Override
    public List<String> listBranches() throws IOException {
        return ((BranchRepository) delegate).listBranches();
    }

    @Override
    public Map<String, BranchStatus> getBranchStatuses(Collection<String> branches) throws IOException {
        return ((BranchRepository) delegate).getBranchStatuses(branches);
    }

    @Override
    public Map<String, BranchTreeRevision> getBranchTreeRevisions(Collection<String> branches,
                                                                  String path) throws IOException {
        branches.forEach(this::validateBranch);
        validatePath(path);
        return ((BranchRepository) delegate).getBranchTreeRevisions(branches, path);
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        validateBranch(branch);
        return new PathCheckedRepository(((BranchRepository) delegate).forBranch(branch), ignoredPaths);
    }

    @Override
    public boolean isValidBranchName(String branch) {
        return ((BranchRepository) delegate).isValidBranchName(branch);
    }

    @Override
    public boolean branchExists(String branch) throws IOException {
        validateBranch(branch);
        return ((BranchRepository) delegate).branchExists(branch);
    }

    @Override
    public MergeOutcome merge(String branchFrom,
                              UserInfo author,
                              ConflictResolveData conflictResolveData) throws IOException {
        validateBranch(branchFrom);
        var outcome = ((BranchRepository) delegate).merge(branchFrom, author, conflictResolveData);
        return new MergeOutcome(outcome.before(),
                outcome.after(),
                shown(outcome.changes(), FileChange::path),
                outcome.workbooks());
    }

    @Override
    public String getBaseBranch() {
        return ((BranchRepository) delegate).getBaseBranch();
    }

    @Override
    public void pull(UserInfo author) throws IOException {
        ((BranchRepository) delegate).pull(author);
    }

    @Override
    public List<FileData> listHistory(String name, String globalFilter, boolean techRevs, Pageable pageable) throws IOException {
        return isIgnored(name)
                ? List.of()
                : ((SearchableRepository) delegate).listHistory(name, globalFilter, techRevs, pageable);
    }

    private void validateBranch(String branch) {
        if (!((BranchRepository) delegate).isValidBranchName(branch)) {
            throw new IllegalArgumentException("Invalide branch name");
        }
    }

}
