package org.openl.rules.repository.api;

public class FeaturesBuilder {

    private boolean folders;
    private boolean branches;
    private boolean versions = true;
    private boolean mappedFolders;
    private boolean uniqueFileId;
    private boolean local;
    private boolean searchable;

    public FeaturesBuilder(Repository repository) {
        this.branches = repository instanceof BranchRepository;
    }

    public FeaturesBuilder setBranches(boolean branches) {
        this.branches = branches;
        return this;
    }

    public FeaturesBuilder setFolders(boolean folders) {
        this.folders = folders;
        return this;
    }

    public FeaturesBuilder setVersions(boolean versions) {
        this.versions = versions;
        return this;
    }

    public FeaturesBuilder setMappedFolders(boolean mappedFolders) {
        this.mappedFolders = mappedFolders;
        return this;
    }

    public FeaturesBuilder setSupportsUniqueFileId(boolean uniqueFileId) {
        this.uniqueFileId = uniqueFileId;
        return this;
    }

    public FeaturesBuilder setLocal(boolean local) {
        this.local = local;
        return this;
    }

    public FeaturesBuilder setSearchable(boolean searchable) {
        this.searchable = searchable;
        return this;
    }

    public Features build() {
        return new Features(folders, branches, versions, mappedFolders, uniqueFileId, local, searchable);
    }
}
