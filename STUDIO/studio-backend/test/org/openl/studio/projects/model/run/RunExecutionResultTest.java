package org.openl.studio.projects.model.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

/**
 * Checks which results a run answers when Studio leaves empty values out of its answers.
 *
 * @author Yury Molchan
 */
class RunExecutionResultTest {

    private final ObjectMapper studioMapper = new ObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);

    @Test
    void answersANullOrEmptyResultButNoResultOfAFailedRun() throws Exception {
        var returnedNull = RunExecutionResult.builder().result(NullNode.getInstance()).build();
        var returnedEmpty = RunExecutionResult.builder().result(studioMapper.createArrayNode()).build();
        var failed = RunExecutionResult.builder().build();

        assertEquals("{\"executionTimeMs\":0.0,\"result\":null}", studioMapper.writeValueAsString(returnedNull));
        assertEquals("{\"executionTimeMs\":0.0,\"result\":[]}", studioMapper.writeValueAsString(returnedEmpty));
        assertEquals("{\"executionTimeMs\":0.0}", studioMapper.writeValueAsString(failed));
    }
}
