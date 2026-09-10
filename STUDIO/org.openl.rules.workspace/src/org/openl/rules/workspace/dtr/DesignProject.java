package org.openl.rules.workspace.dtr;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.project.abstraction.AProject;

/**
 * One design project as a listing reports it, together with the branches that hold it.
 *
 * <p>The project is the view of the home branch, the one a caller that asks for a project alone is given.
 * The branch entries are the same resolution the home was chosen from, so a caller that needs another branch
 * does not have to resolve the project a second time.
 *
 * <p>A project of a repository without branches has none, and so does one served from the configured branch
 * while the cross-branch index is still being built.
 */
@NullMarked
public record DesignProject(AProject project, @Nullable BranchedProject branches) {

    public DesignProject {
        Objects.requireNonNull(project);
    }
}
