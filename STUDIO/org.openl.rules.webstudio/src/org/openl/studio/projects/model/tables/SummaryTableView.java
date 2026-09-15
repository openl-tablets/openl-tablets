package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Summary table view model that contains summarized information about tables
 *
 * @author Vladyslav Pikus
 */
// A row of the list says nothing a whole table read says: what the compiler raised about the table, and
// whether it is written in pieces, are read for the table on screen and not for every row of a rail.
@JsonIgnoreProperties({"messages", "partial"})
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

    @Schema(description = "How many errors the compilation raised about this table. Absent when it raised none, "
            + "so a screen marking the tables that are broken has only the broken ones to read")
    public Integer errors;

    @Schema(description = "Set when a test table exercises this one. Absent when nothing tests it")
    public Boolean hasTests;

    @Schema(description = "Module the table is written in. Answered where the list spans more than one module — "
            + "a search of the project, or of everything the workspace has compiled")
    public String module;

    @Schema(description = "Name of the project that module belongs to, answered with the module")
    public String project;

    @Schema(description = "Identifier of that project, as the Projects API addresses it")
    public String projectId;

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

    /**
     * Says where the table lives: the module it is written in, and the project that module belongs to.
     *
     * <p>Told only where an answer can hold tables of more than one module, because that is where a screen
     * cannot work it out from the request it made.
     */
    public SummaryTableView locatedAt(String module, String project, String projectId) {
        this.module = module;
        this.project = project;
        this.projectId = projectId;
        return this;
    }

    /**
     * Says what the compilation made of the table: how many errors it raised, and whether anything tests it.
     *
     * <p>Neither is said where there is nothing to say — a table that compiled cleanly and a table nothing
     * tests are the ordinary case, and the screen reading the list has only what stands out to read.
     */
    public SummaryTableView reported(int errors, boolean tested) {
        this.errors = errors > 0 ? errors : null;
        this.hasTests = tested ? Boolean.TRUE : null;
        return this;
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
