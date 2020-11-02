package org.openl.rules.repository.api;

public class FeaturesBuilder {
    private final Repository repository;
    private boolean versions = true;
    private boolean mappedFolders;
    private boolean uniqueFileId;
    private boolean local;

    public FeaturesBuilder(Repository repository) {
        this.repository = repository;
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

    public Features build() {
        return new Features(repository, versions, mappedFolders, uniqueFileId, local);
    }
}
