package org.openl.rules.repository.api;

public class FeaturesBuilder {
    private final Repository repository;
    private boolean versions = true;
    private boolean mappedFolders = false;
    private boolean uniqueFileId = false;

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

    public Features build() {
        return new Features(repository, versions, mappedFolders, uniqueFileId);
    }
}