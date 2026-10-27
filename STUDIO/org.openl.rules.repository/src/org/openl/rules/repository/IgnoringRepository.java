package org.openl.rules.repository;

/**
 * A repository that keeps the {@link IgnoredPaths ignored paths} out of reach by itself, where only the repository
 * can, such as for the real paths of a file system.
 *
 * <p>A full save of a folder keeps the ignored files the folder holds, so {@link PathCheckedRepository} passes it on
 * as it is.
 */
public interface IgnoringRepository {

    /**
     * @param setting the ignored paths: Ant-style patterns separated by commas, see {@link IgnoredPaths}
     */
    void setIgnoredPaths(String setting);
}
