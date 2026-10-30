package org.openl.studio.projects.model.tables;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

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

    @Schema(description = """
            Values of the dimension properties that tell this version of the table from the others, inherited ones \
            included, by the technical name of the property in the order of the property definitions. A value keeps \
            its type: a date in ISO 8601, an enum by its name, several values as an array. Absent unless the table \
            has more than one version""")
    public final @Nullable Map<String, Object> dimensions;

    @Schema(description = """
            Signature the versions of one table share. Two tables carry the same value when one is \
            a version of the other. Absent unless the table has more than one version""")
    public final String overloadGroup;

    @Schema(description = """
            Set when the table is switched off by the `active` property, inherited values included. \
            An inactive table is written in the module but takes no part in the rules""")
    public final Boolean active;

    @Schema(description = """
            Category the table is filed under: the one it declares, or else the one it inherits, such \
            as from the file name of its module. Absent when it has none""")
    public final @Nullable String category;

    @Schema(description = """
            How many errors the compilation raised about this table. Absent when it raised none, so a screen \
            marking the tables that are broken has only the broken ones to read""")
    public Integer errors;

    @Schema(description = "Set when a test table exercises this one. Absent when nothing tests it")
    public Boolean hasTests;

    @Schema(description = """
            Module the table is written in. Answered where the list spans more than one module — \
            a search of the project, or of everything the workspace has compiled""")
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
        this.dimensions = builder.dimensions;
        this.overloadGroup = builder.overloadGroup;
        this.active = builder.active;
        this.category = builder.category;
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
        throw new UnsupportedOperationException();
    }

    @Override
    protected int getBodyWidth() {
        throw new UnsupportedOperationException();
    }

    public static Builder builder() {
        return new Builder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder extends TableView.Builder<Builder> {
        private String returnType;
        private String signature;
        private String file;
        private String sheet;
        private String pos;
        private @Nullable Map<String, Object> dimensions;
        private String overloadGroup;
        private Boolean active;
        private @Nullable String category;

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

        public Builder dimensions(Map<String, Object> dimensions) {
            this.dimensions = dimensions;
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

        public Builder category(@Nullable String category) {
            this.category = category;
            return this;
        }

        @Override
        public SummaryTableView build() {
            return new SummaryTableView(this);
        }
    }

}
