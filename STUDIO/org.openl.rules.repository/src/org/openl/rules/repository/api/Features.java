package org.openl.rules.repository.api;

/**
 * Example of usage: if (repository.supports().branches()) return ((BranchRepository) repository).getBranch();
 *
 * @see BranchRepository
 * @see FolderRepository
 * @see FeaturesBuilder
 */
public class Features {
    private final Repository repository;
    private final boolean versions;
    private final boolean mappedFolders;
    private final boolean uniqueFileId;

    /**
     * Don't use this constructor directly. Use {@link FeaturesBuilder} instead.
     */
    Features(Repository repository, boolean versions, boolean mappedFolders, boolean uniqueFileId) {
        this.repository = repository;
        this.versions = versions;
        this.mappedFolders = mappedFolders;
        this.uniqueFileId = uniqueFileId;
    }

    /**
     * If true, repository can be casted to {@link FolderRepository}
     */
    public boolean folders() {
        return repository instanceof FolderRepository;
    }

    /**
     * If true: Repository where each project is mapped to its own Folder. It means that each external folder has it's
     * own internal folder. This repository can manage this mapping. If false: Repository has flat structure. Every
     * project is located inside a single folder.
     */
    public boolean mappedFolders() {
        return mappedFolders;
    }

    /**
     * If true, repository can be casted to {@link BranchRepository}
     */
    public boolean branches() {
        return repository instanceof BranchRepository;
    }

    /**
     * If true, repository can have historic versions. If false, repository is not versionable.
     */
    public boolean versions() {
        return versions;
    }

    /**
     * If true, repository can return unique id for each file (typically it's a hash)
     */
    public boolean uniqueFileId() {
        return uniqueFileId;
    }
}
