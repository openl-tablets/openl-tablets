package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /**
     * The version of a read rides in the ETag header, not in the body; a body that still carries one, as an older
     * client sends it back, is taken in and the version left out of the write.
     */
    @Test
    void leavesTheVersionOutOfTheBody() throws Exception {
        var mapper = new ObjectMapperConfig().objectMapper(new FieldProjectionSupport());
        var read = RawTableView.builder().source(List.of()).build();
        read.version = "v1";

        var json = mapper.writeValueAsString(read);
        var written = mapper.readValue("{\"tableType\": \"RawSource\", \"source\": [], \"version\": \"v1\"}",
                EditableTableView.class);

        assertTrue(mapper.readTree(json).path("version").isMissingNode(), "the ETag header tells the version");
        assertNull(((TableView) written).version, "the If-Match header names the version of a write");
    }
}
