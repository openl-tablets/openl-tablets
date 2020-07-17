package org.openl.rules.repository.git.branch;

public class BranchDescription {
    private String name;
    private String commit;

    public BranchDescription() {
    }

    public BranchDescription(String name, String commit) {
        this.name = name;
        this.commit = commit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }
}
