package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * How a traced cell is highlighted on the client.
 *
 * <p>Shares one vocabulary with the spreadsheet grid and the decision panel: the current line, a
 * returned result, and a passed or failed decision-table condition.
 */
public enum HighlightState {

    @JsonProperty("current")
    CURRENT,

    @JsonProperty("result")
    RESULT,

    @JsonProperty("conditionTrue")
    CONDITION_TRUE,

    @JsonProperty("conditionFalse")
    CONDITION_FALSE
}
