package org.openl.rules.workspace.deploy;

/**
 * This class represents an identifier of a single deployment in production repository. It is immutable.
 */
public class DeployID {
    private String key;

    /**
     * Constructs new <code>DeployID</code> instance with a given String as a key.
     *
     * @param key a string key
     */
    public DeployID(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeployID)) {
            return false;
        }

        DeployID deployID = (DeployID) o;
        return key.equals(deployID.key);
    }

    public String getName() {
        return key;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
