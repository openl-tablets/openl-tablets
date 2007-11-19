package org.openl.rules.workspace.deploy;

public class DeployID {
    private String key;

    public DeployID(String key) {
        this.key = key;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeployID)) return false;

        DeployID deployID = (DeployID) o;
        return key.equals(deployID.key);
    }

    public int hashCode() {
        return key.hashCode();
    }

    public String getName() {
        return key;
    }
}
