package org.openl.rules.repository.git.branch;

import lombok.Getter;
import lombok.Setter;

public class BranchDescription {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String commit;

    public BranchDescription() {
    }

    public BranchDescription(String name, String commit) {
        this.name = name;
        this.commit = commit;
    }
}
