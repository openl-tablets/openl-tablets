package org.openl.rules.repository.api;

/**
 * Example of usage: if (repository.supports().branches()) return ((BranchRepository) repository).getBranch();
 *
 * @see BranchRepository
 * @see FeaturesBuilder
 */
public final class Features {
    private final boolean folders;
    private final boolean branches;
    private final boolean versions;
    private final boolean mappedFolders;
    private final boolean uniqueFileId;
    private final boolean local;

    /**
     * Don't use this constructor directly. Use {@link FeaturesBuilder} instead.
     */
    Features(boolean folders, boolean branches, boolean versions, boolean mappedFolders, boolean uniqueFileId, boolean local) {
        this.folders = folders;
        this.branches = branches;
        this.versions = versions;
        this.mappedFolders = mappedFolders;
        this.uniqueFileId = uniqueFileId;
        this.local = local;
    }

    /**
     * If true, repository can use Folders features
     */
    public boolean folders() {
        return folders;
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
        return branches;
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

    /**
     * If true, repository located in local file system and doesn't support "/deploy" as base path
     */
    public boolean isLocal() {
        return local;
    }
}
