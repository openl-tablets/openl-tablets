package org.openl.studio.projects.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProjectBranchInfoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesOnlyTrueBranchMarks() throws JsonProcessingException {
        var ordinaryBranch = ProjectBranchInfo.builder()
                .name("feature")
                .build();
        var protectedBaseBranch = ProjectBranchInfo.builder()
                .name("main")
                .protectedFlag(true)
                .base(true)
                .build();

        assertEquals("{\"name\":\"feature\"}", mapper.writeValueAsString(ordinaryBranch));
        assertEquals("{\"name\":\"main\",\"protected\":true,\"base\":true}",
                mapper.writeValueAsString(protectedBaseBranch));
    }
}
