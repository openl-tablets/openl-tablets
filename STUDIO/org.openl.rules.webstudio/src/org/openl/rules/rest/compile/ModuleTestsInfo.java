package org.openl.rules.rest.compile;

public class ModuleTestsInfo {

    private final Integer count;
    private final Boolean compiled;
    private final TableRunState tableRunState;

    public ModuleTestsInfo(Builder from) {
        this.count = from.count;
        this.compiled = from.compiled;
        this.tableRunState = from.tableRunState;
    }

    public int getCount() {
        return count;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public TableRunState getTableRunState() {
        return tableRunState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer count;
        private Boolean compiled;
        private TableRunState tableRunState;

        private Builder() {
        }

        public Builder count(Integer count) {
            this.count = count;
            return this;
        }

        public Builder compiled(Boolean compiled) {
            this.compiled = compiled;
            return this;
        }

        public Builder tableRunState(TableRunState tableRunState) {
            this.tableRunState = tableRunState;
            return this;
        }

        public ModuleTestsInfo build() {
            return new ModuleTestsInfo(this);
        }
    }

}
