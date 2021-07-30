package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;

public final class ProjectCompilationStatus {

    private final int modulesCount;

    private final int modulesCompiled;

    private final Map<Severity, List<OpenLMessage>> messages;

    private ProjectCompilationStatus(Builder builder) {
        this.modulesCount = builder.modulesCount;
        this.modulesCompiled = builder.modulesCompiled;
        HashMap<Severity, List<OpenLMessage>> messagesMap = new HashMap<>();
        builder.messages.forEach((key, value) -> messagesMap.put(key, Collections.unmodifiableList(value)));
        this.messages = Collections.unmodifiableMap(messagesMap);
    }

    public int getModulesCount() {
        return modulesCount;
    }

    public int getModulesCompiled() {
        return modulesCompiled;
    }

    public int getWarningsCount() {
        return getMessagesCount(Severity.WARN);
    }

    public int getErrorsCount() {
        return getMessagesCount(Severity.ERROR);
    }

    private int getMessagesCount(Severity severity) {
        return Optional.ofNullable(messages.get(severity)).map(List::size).orElse(0);
    }

    public Collection<OpenLMessage> getAllMessage() {
        return messages.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static Builder newBuilder() {
        return new ProjectCompilationStatus.Builder();
    }

    public static class Builder {

        private int modulesCount = 0;

        private int modulesCompiled = 0;

        private final Map<Severity, List<OpenLMessage>> messages = new HashMap<>();

        private final Set<OpenLMessage> uniqueMessages = new HashSet<>();

        public Builder addModulesCount(Integer count) {
            modulesCount += count;
            return this;
        }

        public Builder addModulesCompiled(Integer count) {
            modulesCompiled += count;
            return this;
        }

        public Builder setModulesCompiled(Integer modulesCompiled) {
            this.modulesCompiled = modulesCompiled;
            return this;
        }

        public Builder addMessage(OpenLMessage message) {
            if (uniqueMessages.add(message)) {
                messages.computeIfAbsent(message.getSeverity(), e -> new ArrayList<>()).add(message);
            }
            return this;
        }

        public Builder addMessages(Collection<OpenLMessage> messages) {
            if (messages != null) {
                messages.forEach(this::addMessage);
            }
            return this;
        }

        public Builder clearMessages() {
            messages.clear();
            uniqueMessages.clear();
            return this;
        }

        public ProjectCompilationStatus build() {
            return new ProjectCompilationStatus(this);
        }
    }
}
