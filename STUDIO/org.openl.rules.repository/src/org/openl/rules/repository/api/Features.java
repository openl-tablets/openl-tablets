package org.openl.rules.repository.api;

/**
 * Example of usage:
 * if (repository.supports().branches()) return ((BranchRepository) repository).getBranch();
 *
 * @see BranchRepository
 * @see FolderRepository
 */
public class Features {
    private final Repository repository;

    public Features(Repository repository) {
        this.repository = repository;
    }

    /**
     * If true, repository can be casted to {@link FolderRepository}
     */
    public boolean folders() {
        return repository instanceof FolderRepository;
    }

    /**
     * If true, repository can be casted to {@link MappedFolderRepository}
     */
    public boolean mappedFolders() {
        return repository instanceof MappedFolderRepository;
    }

    /**
     * If true, repository can be casted to {@link BranchRepository}
     */
    public boolean branches() {
        return repository instanceof BranchRepository;
    }
}
