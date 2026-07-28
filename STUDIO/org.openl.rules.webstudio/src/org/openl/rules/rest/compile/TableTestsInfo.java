package org.openl.rules.rest.compile;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import org.openl.rules.webstudio.web.tableeditor.TableBean;

public class TableTestsInfo {

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<TableBean.TableDescription> allTests;

    private final Boolean compiled;

    public TableTestsInfo(Builder from) {
        this.allTests = from.allTests;
        this.compiled = from.compiled;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<TableBean.TableDescription> allTests;
        private Boolean compiled;

        private Builder() {
        }

        public Builder allTests(List<TableBean.TableDescription> allTests) {
            this.allTests = allTests;
            return this;
        }

        public Builder compiled(Boolean compiled) {
            this.compiled = compiled;
            return this;
        }

        public TableTestsInfo build() {
            return new TableTestsInfo(this);
        }
    }

}
