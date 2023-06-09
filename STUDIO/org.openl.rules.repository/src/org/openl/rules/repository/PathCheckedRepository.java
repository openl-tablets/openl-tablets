package org.openl.rules.repository;

import static org.openl.rules.repository.api.Repository.validatePath;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.SearchableRepository;
import org.openl.rules.repository.api.UserInfo;

/**
 * A wrapper class to check arguments against path traversal vulnerability in the Repository API.
 * It solves the issue only, if the Repository instantiated via {@code org.openl.rules.repository.RepositoryInstatiator}
 *
 * @author Yury Molchan
 */
public class PathCheckedRepository implements BranchRepository {

    private final Repository delegate;

    PathCheckedRepository(Repository delegate) {
        this.delegate = delegate;
    }

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
        return delegate.list(path);
    }

    @Override
    public FileData check(String name) throws IOException {
        validatePath(name);
        return delegate.check(name);
    }

    @Override
    public FileItem read(String name) throws IOException {
        validatePath(name);
        return delegate.read(name);
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        validatePath(data.getName());
        return delegate.save(data, stream);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        fileItems.forEach(x -> validatePath(x.getData().getName()));
        return delegate.save(fileItems);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        validatePath(data.getName());
        return delegate.delete(data);
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        data.forEach(x -> validatePath(x.getName()));
        return delegate.delete(data);
    }

    @Override
    public void setListener(Listener callback) {
        delegate.setListener(callback);
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        validatePath(name);
        return delegate.listHistory(name);
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        validatePath(name);
        return delegate.checkHistory(name, version);
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        validatePath(name);
        return delegate.readHistory(name, version);
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        validatePath(data.getName());
        return delegate.deleteHistory(data);
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        validatePath(srcName);
        validatePath(destData.getName());
        return delegate.copyHistory(srcName, destData, version);
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        validatePath(path);
        return delegate.listFolders(path);
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        validatePath(path);
        return delegate.listFiles(path, version);
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException {
        validatePath(folderData.getName());
        // FIXME: this Iterable cannot be used multiple times due delegating it to the Stream API.
        //     It needs to refactor the Repository API to allow the following:
        //     files.peek(x -> validatePath(x.getData().getName()));
        return delegate.save(folderData, files, changesetType);
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
    public String getBranch() {
        return ((BranchRepository) delegate).getBranch();
    }

    @Override
    public boolean isBranchProtected(String branch) {
        return ((BranchRepository) delegate).isBranchProtected(branch);
    }

    @Override
    public void createBranch(String projectPath, String branch) throws IOException {
        validatePath(projectPath);
        validateBranch(branch);
        ((BranchRepository) delegate).createBranch(projectPath, branch);
    }

    @Override
    public void deleteBranch(String projectPath, String branch) throws IOException {
        validatePath(projectPath);
        validateBranch(branch);
        ((BranchRepository) delegate).deleteBranch(projectPath, branch);
    }

    @Override
    public List<String> getBranches(String projectPath) throws IOException {
        validatePath(projectPath);
        return ((BranchRepository) delegate).getBranches(projectPath);
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        validateBranch(branch);
        return ((BranchRepository) delegate).forBranch(branch);
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
    public void merge(String branchFrom, UserInfo author, ConflictResolveData conflictResolveData) throws IOException {
        validateBranch(branchFrom);
        ((BranchRepository) delegate).merge(branchFrom, author, conflictResolveData);
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
        return ((SearchableRepository) delegate).listHistory(name, globalFilter, techRevs, pageable);
    }

    private void validateBranch(String branch) {
        if (!((BranchRepository) delegate).isValidBranchName(branch)) {
            throw new IllegalArgumentException("Invalide branch name");
        }
    }
}
