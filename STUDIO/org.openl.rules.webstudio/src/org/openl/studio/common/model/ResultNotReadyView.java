package org.openl.studio.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;

/**
 * The answer to a request for what a task produced, while the task has not ended yet.
 *
 * <p>A task that goes on in the background - a run, a test run, a benchmark, a comparison - has nothing to
 * report until it ends. The request is accepted rather than refused: nothing is wrong, the result is not ready.
 * A screen asking after the result raises no error in the browser this way, and asks again once the status of
 * the task says it has ended.
 */
@Schema(description = "The result asked for is not ready: the task producing it has not ended yet")
public record ResultNotReadyView(
        @Parameter(description = "Why there is nothing to report: `notReady` while the task goes on")
        ResultState status) {

    private static final ResultNotReadyView NOT_READY = new ResultNotReadyView(ResultState.NOT_READY);

    /** The state a result is in while the task producing it goes on. */
    public enum ResultState {
        @JsonProperty("notReady")
        NOT_READY
    }

    /** The response of a result endpoint while the task goes on: accepted, with the state of the result. */
    public static ResponseEntity<ResultNotReadyView> accepted() {
        return ResponseEntity.accepted().body(NOT_READY);
    }
}
