package org.openl.studio.tags.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * What filling tags from the project name templates did to one project it was asked for.
 *
 * @param projectName the project's business name, as the templates match it
 * @param outcome     what filling did to the project
 * @param tags        the tag values the project got, by tag type; empty when it was left alone
 * @param rejected    the missing values the project could not get, by tag type: they are not in the list of tags,
 *                    and their tag types do not take them
 * @param blocker     why the project could not be changed, when it was not
 *
 * @author Yury Molchan
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "What filling tags from the project name templates did to one project")
public record TagFillResult(
        @Parameter(description = "Project business name")
        String projectName,
        @Parameter(description = "What filling did to the project")
        TagFillOutcome outcome,
        @Parameter(description = "The tag values the project got, by tag type")
        Map<String, String> tags,
        @Parameter(description = "The missing tag values the project could not get, by tag type")
        Map<String, String> rejected,
        @Parameter(description = "Why the project could not be changed")
        @Nullable TagFillBlocker blocker) {

    /** The project got the given values, and could not get the rejected ones. */
    public static TagFillResult updated(String projectName, Map<String, String> tags, Map<String, String> rejected) {
        return new TagFillResult(projectName, TagFillOutcome.UPDATED, tags, rejected, null);
    }

    /** The project could not be changed now, for the given reason. */
    public static TagFillResult notModifiable(String projectName, TagFillBlocker blocker) {
        return new TagFillResult(projectName, TagFillOutcome.NOT_MODIFIABLE, Map.of(), Map.of(), blocker);
    }

    /** None of the values the project lacks could be assigned. */
    public static TagFillResult nothingToAssign(String projectName, Map<String, String> rejected) {
        return new TagFillResult(projectName, TagFillOutcome.NOTHING_TO_ASSIGN, Map.of(), rejected, null);
    }

    /** The tags file of the project could not be written. */
    public static TagFillResult failed(String projectName) {
        return new TagFillResult(projectName, TagFillOutcome.FAILED, Map.of(), Map.of(), null);
    }
}
