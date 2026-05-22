package org.openl.studio.projects.service;

import java.util.Collection;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;

/**
 * Maps {@link OpenLMessage}s to {@link DetailedMessageDescription}s, resolving each
 * message's origin (module or table) against the supplied {@link ProjectModel} so the
 * client can deep-link straight to the offending cell.
 *
 * <p>The result is sorted using the same canonical ordering as
 * {@link MessageDescriptionMapper}: by severity, then by id.</p>
 *
 * @author Vladyslav Pikus
 */
public interface DetailedMessageDescriptionMapper {

    /**
     * Map every input message and return the result sorted by severity then by id.
     */
    List<DetailedMessageDescription> mapSorted(Collection<OpenLMessage> messages, ProjectModel model);
}
