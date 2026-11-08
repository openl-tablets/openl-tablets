package org.openl.studio.projects.service;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.MessagePositions;

/**
 * Maps {@link OpenLMessage}s to {@link DetailedMessageDescription}s, resolving each
 * message's origin (module or table) against the supplied {@link ProjectModel} so the
 * client can deep-link straight to the offending cell.
 *
 * <p>The result is sorted using the canonical ordering of compilation messages in REST responses:
 * by severity, then by id.</p>
 *
 * @author Vladyslav Pikus
 */
public interface DetailedMessageDescriptionMapper {

    /**
     * Map every input message and return the result sorted by severity then by id.
     */
    List<DetailedMessageDescription> mapSorted(Collection<OpenLMessage> messages, ProjectModel model);

    /**
     * Map the messages about the files of a project, sorted as {@link #mapSorted} sorts them. A file is named by its
     * path in the project folder; no compiled project is needed for it.
     *
     * <p>A message about a file tells its places counted as the positions say: in the text of its key, or in the file.
     * {@link #mapSorted} tells them in the file.
     */
    List<DetailedMessageDescription> mapFileMessages(Collection<OpenLMessage> messages,
                                                     Path projectFolder,
                                                     MessagePositions positions);
}
