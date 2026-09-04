package org.openl.rules.rest.compile;

import java.util.List;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import org.openl.rules.webstudio.web.tableeditor.TableBean;

public class TableInfo {

    @Getter
    private final List<OpenlProblemMessage> errors;
    @Getter
    private final List<OpenlProblemMessage> warnings;
    @Getter
    private final List<Pair<String, TableBean.TableDescription>> targetTables;
    @Getter
    private final String tableUrl;
    @Getter
    private final TableRunState tableRunState;

    public TableInfo(Builder from) {
        this.errors = from.errors;
        this.warnings = from.warnings;
        this.targetTables = from.targetTables;
        this.tableUrl = from.tableUrl;
        this.tableRunState = from.tableRunState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<OpenlProblemMessage> errors;
        private List<OpenlProblemMessage> warnings;
        private List<Pair<String, TableBean.TableDescription>> targetTables;
        private String tableUrl;
        private TableRunState tableRunState;

        private Builder() {
        }

        public Builder errors(List<OpenlProblemMessage> errors) {
            this.errors = errors;
            return this;
        }

        public Builder warnings(List<OpenlProblemMessage> warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder targetTables(List<Pair<String, TableBean.TableDescription>> targetTables) {
            this.targetTables = targetTables;
            return this;
        }

        public Builder tableUrl(String tableUrl) {
            this.tableUrl = tableUrl;
            return this;
        }

        public Builder tableRunState(TableRunState tableRunState) {
            this.tableRunState = tableRunState;
            return this;
        }

        public TableInfo build() {
            return new TableInfo(this);
        }
    }
}
