package org.openl.studio.projects.model.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.studio.common.projection.FieldProjectionSupport;
import org.openl.studio.config.ObjectMapperConfig;
import org.openl.studio.projects.model.tables.RawTableCell;

class ConflictWorkbookViewTest {

    @Test
    void keepsWhatAScreenReadsEvenWhenItIsEmptyOrFalse() throws Exception {
        var mapper = new ObjectMapperConfig().objectMapper(new FieldProjectionSupport());
        var base = new ConflictTableVersion("A1:A1",
                List.of(List.of(RawTableCell.builder().cell("A1").build())),
                List.of(),
                1,
                List.of(),
                false,
                List.of());
        var view = new ConflictWorkbookView(List.of(
                new ConflictSheetView("Rates", ConflictSheetStatus.THEIRS, null, true, true, true, List.of()),
                new ConflictSheetView("Rules",
                        ConflictSheetStatus.CONFLICT,
                        null,
                        true,
                        true,
                        false,
                        List.of(new ConflictTableView("Rules/A1", "A1", ConflictTableReason.ADDED_IN_BOTH, base, null,
                                null, null)))), false);

        assertEquals("""
                {"sheets":[\
                {"name":"Rates","status":"theirs","ours":true,"base":true,"theirs":true,"tables":[]},\
                {"name":"Rules","status":"conflict","ours":true,"base":true,"theirs":false,"tables":[\
                {"id":"Rules/A1","name":"A1","reason":"addedInBoth",\
                "ours":{"range":"A1:A1","cells":[[{"cell":"A1"}]],"styles":[],"rows":1,"changedCells":[],\
                "formattingChanged":false,"conflictCells":[]}}]}],\
                "palette":false}""", mapper.writeValueAsString(view));
    }
}
