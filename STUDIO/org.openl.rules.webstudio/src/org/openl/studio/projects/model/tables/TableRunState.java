package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether the table can be run as it stands, and how far a run of it may reach.
 *
 * @author Vladyslav Pikus
 */
@Schema(description = "Whether the table can be run, and how far a run of it may reach")
public enum TableRunState {

    /** Nothing stands in the way: the table runs against the whole project. */
    @JsonProperty("can-run")
    CAN_RUN,

    /**
     * Runnable, but only against the module it is written in — the rest of the project is not built, or what
     * is built beyond this module has errors.
     */
    @JsonProperty("can-run-module")
    CAN_RUN_MODULE,

    /**
     * Not runnable: the table itself failed to compile, or — for a test or a run table — the rules it
     * exercises did.
     */
    @JsonProperty("cannot-run")
    CANNOT_RUN
}
