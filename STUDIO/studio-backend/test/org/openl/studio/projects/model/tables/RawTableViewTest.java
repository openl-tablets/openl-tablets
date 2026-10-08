package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.studio.common.projection.FieldProjectionSupport;
import org.openl.studio.config.ObjectMapperConfig;

class RawTableViewTest {

    @Test
    void writesTheEmptyMatrixOfAWindowPastTheLastRow() throws Exception {
        var mapper = new ObjectMapperConfig().objectMapper(new FieldProjectionSupport());
        var view = RawTableView.builder().source(List.of()).totalRows(5).build();

        var json = mapper.readTree(mapper.writeValueAsString(view));

        assertTrue(json.path("source").isArray(), "the empty matrix is written, not left out");
        assertEquals(0, json.path("source").size());
        assertEquals(5, json.path("totalRows").asInt());
    }
}
