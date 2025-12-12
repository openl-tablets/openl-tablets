package org.openl.studio.projects.model.tests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonDeserialize(builder = TestExecutionSummaryQuery.Builder.class)
public record TestExecutionSummaryQuery(
        @Schema(description = "If true, only failed test units are included in the summary. Default is false.")
        boolean failedOnly,

        @Schema(description = "The maximum number of failed test units to include in the summary. Default is 5.")
        int failures
) {

    private static final TestExecutionSummaryQuery NO_FILTER = new Builder().build();

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    public static TestExecutionSummaryQuery noFilter() {
        return NO_FILTER;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private boolean failedOnly;
        private int failures = 5;

        public Builder failedOnly(boolean failedOnly) {
            this.failedOnly = failedOnly;
            return this;
        }

        public Builder failures(int failures) {
            this.failures = failures;
            return this;
        }

        public TestExecutionSummaryQuery build() {
            return new TestExecutionSummaryQuery(failedOnly, failures);
        }
    }

}
