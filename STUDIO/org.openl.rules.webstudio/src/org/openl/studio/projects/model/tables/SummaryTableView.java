package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Summary table view model that contains summarized information about tables
 *
 * @author Vladyslav Pikus
 */
public class SummaryTableView extends TableView {

    @Schema(description = "Return type of the table (e.g., Integer, String, etc.)")
    public final String returnType;

    @Schema(description = "Signature of the table")
    public final String signature;

    @Schema(description = "File where the table is located")
    public final String file;

    @Schema(description = "Position of the table within the file")
    public final String pos;

    private SummaryTableView(Builder builder) {
        super(builder);
        this.returnType = builder.returnType;
        this.signature = builder.signature;
        this.file = builder.file;
        this.pos = builder.pos;
    }

    @Override
    protected int getBodyHeight() {
        throw  new UnsupportedOperationException();
    }

    @Override
    protected int getBodyWidth() {
        throw  new UnsupportedOperationException();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends TableView.Builder<Builder> {
        private String returnType;
        private String signature;
        private String file;
        private String pos;

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder file(String file) {
            this.file = file;
            return this;
        }

        public Builder pos(String pos) {
            this.pos = pos;
            return this;
        }

        public SummaryTableView build() {
            return new SummaryTableView(this);
        }
    }

}
