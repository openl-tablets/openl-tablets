package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;

import com.google.common.collect.ImmutableMap;

public final class ProjectCompilationStatus {

    private final int warningsCount;

    private final int errorsCount;

    private final int modulesCount;

    private final int modulesCompiled;

    private final List<OpenLMessage> messages;

    private ProjectCompilationStatus(Builder builder) {
        this.warningsCount = builder.warningsCount;
        this.errorsCount = builder.errorsCount;
        this.modulesCount = builder.modulesCount;
        this.modulesCompiled = builder.modulesCompiled;
        this.messages = Collections.unmodifiableList(builder.messages);
    }

    public int getWarningsCount() {
        return warningsCount;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public int getModulesCount() {
        return modulesCount;
    }

    public int getModulesCompiled() {
        return modulesCompiled;
    }

    public List<OpenLMessage> getMessages() {
        return messages;
    }

    public static Builder newBuilder() {
        return new ProjectCompilationStatus.Builder();
    }

    public static class Builder {

        private int warningsCount = 0;

        private int errorsCount = 0;

        private int modulesCount = 0;

        private int modulesCompiled = 0;

        private final List<OpenLMessage> messages = new ArrayList<>();

        private final Set<OpenLMessage> uniqueMessages = new HashSet<>();

        private final Map<Severity, Consumer<OpenLMessage>> messageConsumers = ImmutableMap
            .<Severity, Consumer<OpenLMessage>> builder()
            .put(Severity.WARN, (message) -> {
                warningsCount++;
                messages.add(message);
            })
            .put(Severity.ERROR, (message) -> {
                errorsCount++;
                messages.add(message);
            })
            .build();

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
                Optional.ofNullable(messageConsumers.get(message.getSeverity()))
                    .ifPresent(openLMessageConsumer -> openLMessageConsumer.accept(message));
            }
            return this;
        }

        public Builder addMessages(Collection<OpenLMessage> messages) {
            Optional.ofNullable(messages).ifPresent(messagesValue -> messagesValue.forEach(this::addMessage));
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
