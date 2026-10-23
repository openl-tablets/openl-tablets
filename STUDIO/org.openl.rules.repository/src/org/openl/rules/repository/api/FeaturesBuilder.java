package org.openl.rules.repository.api;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class FeaturesBuilder {

    private boolean folders;
    private boolean branches;
    private boolean versions = true;
    private boolean mappedFolders;
    @Setter(AccessLevel.NONE)
    private boolean uniqueFileId;
    private boolean local;
    private boolean searchable;
    private boolean comments = true;

    public FeaturesBuilder(Repository repository) {
        this.branches = repository instanceof BranchRepository;
    }

    public FeaturesBuilder setSupportsUniqueFileId(boolean uniqueFileId) {
        this.uniqueFileId = uniqueFileId;
        return this;
    }

    public Features build() {
        return new Features(folders, branches, versions, mappedFolders, uniqueFileId, local, searchable, comments);
    }
}
