package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Test table model for getting and updating Test tables
 *
 * Header format: "Test <testedTableName> <testName>"
 * Example: "Test BankLimitIndex BankLimitIndexTest" where
 *   - testedTableName = "BankLimitIndex" (the table being tested)
 *   - name = "BankLimitIndexTest" (inherited from TableView, the test name)
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = TestView.Builder.class)
public class TestView extends AbstractDataView implements EditableTableView {

    public static final String TABLE_TYPE = "Test";

    @Schema(description = "Name of the table being tested by this test table")
    public final String testedTableName;

    private TestView(Builder builder) {
        super(builder);
        this.testedTableName = builder.testedTableName;
    }

    @Override
    public String getTableType() {
        return tableType;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends AbstractDataView.Builder<Builder> {
        private String testedTableName;

        private Builder() {
            tableType(TABLE_TYPE);
        }

        public Builder testedTableName(String testedTableName) {
            this.testedTableName = testedTableName;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public TestView build() {
            return new TestView(this);
        }
    }

}
