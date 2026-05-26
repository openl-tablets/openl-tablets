package org.openl.studio.projects.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import org.openl.message.OpenLMessage;
import org.openl.rules.rest.compile.MessageDescription;

@Service
public class MessageDescriptionMapperImpl implements MessageDescriptionMapper {

    private static final Comparator<MessageDescription> BY_SEVERITY_AND_ID = Comparator
            .comparing(MessageDescription::severity)
            .thenComparing(MessageDescription::id);

    @Override
    public List<MessageDescription> mapSorted(Collection<OpenLMessage> messages) {
        return messages.stream()
                .map(MessageDescriptionMapperImpl::map)
                .sorted(BY_SEVERITY_AND_ID)
                .toList();
    }

    private static MessageDescription map(OpenLMessage message) {
        return new MessageDescription(message.getId(), message.getSummary(), message.getSeverity());
    }
}
