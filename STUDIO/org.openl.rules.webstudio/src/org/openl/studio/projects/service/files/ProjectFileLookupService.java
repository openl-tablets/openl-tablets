package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.projects.model.files.FsNode;

/**
 * Finds files by name along the anchor's ancestor line.
 *
 * <p>Given an anchor path, the lookup walks up from the anchor folder to the repository root and
 * collects the file whose name equals the anchor's file name found at each level — the anchor itself,
 * then each ancestor. Matches are returned nearest to the anchor first. The walk goes up only:
 * descendants of the anchor and sibling branches are not visited. It crosses the project boundary, so
 * ancestors above the anchor's project are reached too.
 *
 * <p>Only text files within a size bound are surfaced. Authorization for the mount is enforced by the
 * caller before the lookup runs.
 */
public interface ProjectFileLookupService {

    /**
     * Collects same-named files for a project mount. Files inside the project are resolved through its
     * artefact tree (reflecting the working copy and unpacking flat projects); files outside are read
     * from the repository when it exposes a folder hierarchy above the project.
     *
     * @param project        the project the anchor belongs to, providing the in-project artefact tree
     * @param repository     the design repository to scan outside the project (folder repositories
     *                       only); {@code null} for a local-only project, which keeps the search within
     *                       the project. For an open project this is not its working-copy repository.
     * @param anchorPath     repository-relative path whose file name is matched and whose folder is
     *                       the proximity anchor (e.g. {@code "rating/config/AGENTS.md"})
     * @param includeContent whether to read and include the raw file content (UTF-8) in each node
     * @return matching files ordered nearest to farthest from the anchor; empty if nothing was found
     */
    List<FsNode> lookup(@NotNull AProject project,
                        Repository repository,
                        @NotBlank String anchorPath,
                        boolean includeContent) throws IOException;

    /**
     * Collects same-named files for a repository mount, walking up from the anchor to the repository
     * root. Used where no project context exists; the repository must expose a folder hierarchy.
     *
     * @param repository     the repository to search; mapped or delegating wrappers are unwrapped to
     *                       address files by their real, repository-relative paths
     * @param anchorPath     repository-relative path whose file name is matched and whose folder is
     *                       the proximity anchor
     * @param includeContent whether to read and include the raw file content (UTF-8) in each node
     * @return matching files ordered nearest to farthest from the anchor; empty if nothing was found
     */
    List<FsNode> lookup(@NotNull Repository repository,
                        @NotBlank String anchorPath,
                        boolean includeContent) throws IOException;
}
