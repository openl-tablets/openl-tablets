package org.openl.rules.rest.compile;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class CompileModuleInfo {

    private final String dataType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<MessageDescription> messages;
    private final Long messageId;
    private final Integer messageIndex;
    private final Integer errorsCount;
    private final Integer warningsCount;
    private final Integer modulesCount;
    private final Integer modulesCompiled;
    private final Boolean compilationCompleted;

    public CompileModuleInfo(Builder from) {
        this.dataType = from.dataType;
        this.messages = from.messages;
        this.messageId = from.messageId;
        this.messageIndex = from.messageIndex;
        this.errorsCount = from.errorsCount;
        this.warningsCount = from.warningsCount;
        this.modulesCount = from.modulesCount;
        this.modulesCompiled = from.modulesCompiled;
        this.compilationCompleted = from.compilationCompleted;
    }

    public String getDataType() {
        return dataType;
    }

    public List<MessageDescription> getMessages() {
        return messages;
    }

    public long getMessageId() {
        return messageId;
    }

    public int getMessageIndex() {
        return messageIndex;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public int getWarningsCount() {
        return warningsCount;
    }

    public int getModulesCount() {
        return modulesCount;
    }

    public int getModulesCompiled() {
        return modulesCompiled;
    }

    public boolean isCompilationCompleted() {
        return compilationCompleted;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String dataType;
        private List<MessageDescription> messages;
        private Long messageId;
        private Integer messageIndex;
        private Integer errorsCount;
        private Integer warningsCount;
        private Integer modulesCount;
        private Integer modulesCompiled;
        private Boolean compilationCompleted;

        private Builder() {
        }

        public Builder dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder messages(List<MessageDescription> messages) {
            this.messages = messages;
            return this;
        }

        public Builder messageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder messageIndex(Integer messageIndex) {
            this.messageIndex = messageIndex;
            return this;
        }

        public Builder errorsCount(Integer errorsCount) {
            this.errorsCount = errorsCount;
            return this;
        }

        public Builder warningsCount(Integer warningsCount) {
            this.warningsCount = warningsCount;
            return this;
        }

        public Builder modulesCount(Integer modulesCount) {
            this.modulesCount = modulesCount;
            return this;
        }

        public Builder modulesCompiled(Integer modulesCompiled) {
            this.modulesCompiled = modulesCompiled;
            return this;
        }

        public Builder compilationCompleted(Boolean compilationCompleted) {
            this.compilationCompleted = compilationCompleted;
            return this;
        }

        public CompileModuleInfo build() {
            return new CompileModuleInfo(this);
        }
    }

}
