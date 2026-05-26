package org.openl.studio.projects.service;

import java.util.Collection;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.rules.rest.compile.MessageDescription;

/**
 * Maps {@link OpenLMessage}s to {@link MessageDescription}s and applies the canonical
 * ordering used for compilation messages in REST responses: by severity, then by id.
 *
 * @author Vladyslav Pikus
 */
public interface MessageDescriptionMapper {

    /**
     * Map every input message and return the result sorted by severity then by id.
     */
    List<MessageDescription> mapSorted(Collection<OpenLMessage> messages);
}
