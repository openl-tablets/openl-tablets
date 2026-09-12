package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Summary table view model that contains summarized information about tables
 *
 * @author Vladyslav Pikus
 */
@JsonIgnoreProperties("messages")
public class SummaryTableView extends TableView {

    @Schema(description = "Return type of the table (e.g., Integer, String, etc.)")
    public final String returnType;

    @Schema(description = "Signature of the table")
    public final String signature;

    @Schema(description = "File where the table is located")
    public final String file;

    @Schema(description = "Excel sheet the table is written on")
    public final String sheet;

    @Schema(description = "Position of the table within the file")
    public final String pos;

    @Schema(description = "Name that tells this version of the table from the others: the table name followed by "
            + "the dimension properties that decide which version answers. Absent unless the table has more than "
            + "one version")
    public final String displayName;

    @Schema(description = "Signature the versions of one table share. Two tables carry the same value when one is "
            + "a version of the other. Absent unless the table has more than one version")
    public final String overloadGroup;

    @Schema(description = "Set when the table is switched off by the `active` property, inherited values included. "
            + "An inactive table is written in the module but takes no part in the rules")
    public final Boolean active;

    protected SummaryTableView(Builder builder) {
        super(builder);
        this.returnType = builder.returnType;
        this.signature = builder.signature;
        this.file = builder.file;
        this.sheet = builder.sheet;
        this.pos = builder.pos;
        this.displayName = builder.displayName;
        this.overloadGroup = builder.overloadGroup;
        this.active = builder.active;
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
        private String sheet;
        private String pos;
        private String displayName;
        private String overloadGroup;
        private Boolean active;

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

        public Builder sheet(String sheet) {
            this.sheet = sheet;
            return this;
        }

        public Builder pos(String pos) {
            this.pos = pos;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder overloadGroup(String overloadGroup) {
            this.overloadGroup = overloadGroup;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        @Override
        public SummaryTableView build() {
            return new SummaryTableView(this);
        }
    }

}
