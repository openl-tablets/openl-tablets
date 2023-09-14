package org.openl.rules.rest.model.tables;

/**
 * Table info model.
 *
 * @author Vladyslav Pikus
 */
public class SummaryTableView extends TableView {

    public final String returnType;
    public final String signature;
    public final String file;
    public final String pos;

    private SummaryTableView(Builder builder) {
        super(builder);
        this.returnType = builder.returnType;
        this.signature = builder.signature;
        this.file = builder.file;
        this.pos = builder.pos;
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
