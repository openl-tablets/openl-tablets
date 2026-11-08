package org.openl.studio.projects.service.project.status;

import java.nio.file.Path;
import java.util.Collection;
import jakarta.annotation.Nullable;

import org.openl.message.OpenLMessage;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;

/**
 * Maps a {@link RulesProject} to a {@link ProjectStatusViewModel}.
 *
 * @author Vladyslav Pikus
 */
public interface ProjectStatusMapper {

    /**
     * Build a status view for the given project. The compilation job is looked up via the
     * client-scoped registry, so this overload must be called from a thread that has the
     * web session bound (typically a REST request thread).
     *
     * @param project workspace project to inspect
     * @return populated status view
     */
    ProjectStatusViewModel map(RulesProject project);

    /**
     * Build a summary status view for a projects-list row. It reports the same state and message
     * counts as {@link #map(RulesProject)}, but omits the detailed compilation message list, which
     * the list does not display. Skipping it avoids resolving every message to its table and module,
     * the dominant cost when a large project raises thousands of messages.
     *
     * @param project workspace project to inspect
     * @return status view without the detailed compilation message list
     */
    ProjectStatusViewModel mapSummary(RulesProject project);

    /**
     * Build a status view for the given project using the supplied {@link ProjectModel}
     * directly, without consulting the client-scoped compilation registry. Use this
     * overload when the model is already known (e.g. from a Spring event handler running
     * outside a request context).
     *
     * @param project workspace project to inspect
     * @param model   project model to read compilation info from; {@code null} means no
     *                compilation has been initiated (status will be {@code IDLE})
     * @return populated status view
     */
    ProjectStatusViewModel map(RulesProject project, @Nullable ProjectModel model);

    /**
     * Build a status view reporting how far a running compilation has come.
     *
     * <p>It carries the compile state and the module counts and names, and leaves out what a compilation in
     * progress cannot answer cheaply: every message resolved to its table, and the changes not committed yet,
     * which a compilation does not touch anyway.
     *
     * <p>This is the flavour raised from a thread that holds the model while it compiles, so the reader sees
     * the progress as it happens rather than all at once at the end.
     *
     * @param project workspace project to inspect
     * @param model   project model to read compilation info from
     * @return status view carrying the progress of the compilation
     */
    ProjectStatusViewModel mapProgress(RulesProject project, @Nullable ProjectModel model);

    /**
     * The status of a project nobody compiled, with what the check of its localization files found. The check needs
     * no compilation, and a compilation tells the same as its first step.
     *
     * <p>A status that tells a compilation is returned as it is, and so is one without warnings to add.
     *
     * @param status        the status of the project
     * @param messages      what the check of the localization files found
     * @param projectFolder the folder of the project, which the files are named relative to
     * @param items         whether the messages are listed, or only counted as in the list of the projects
     * @return the status with the messages of the check
     */
    ProjectStatusViewModel withFileChecks(ProjectStatusViewModel status,
                                          Collection<OpenLMessage> messages,
                                          Path projectFolder,
                                          boolean items);
}
