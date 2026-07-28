package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that bean validation cascades into every cell of the raw create/update ({@link RawTableView}) and
 * append ({@link RawTableAppend}) models, that an empty matrix is rejected, and that a compiled table header must
 * name a table type OpenL recognizes. An explicitly free-form table accepts an arbitrary non-blank header.
 */
class RawTableValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static RawTableView rawTable(Object cellValue) {
        // a recognized header in the first cell keeps these cases focused on cell-value validation, not the header
        return RawTableView.builder()
                .source(List.of(List.of(
                        RawTableCell.builder().value("Datatype").build(),
                        RawTableCell.builder().value(cellValue).build())))
                .build();
    }

    @Test
    void rejectsNonScalarCellInUpdateMatrix() {
        assertEquals(1, validator.validate(rawTable(Map.of("a", 1))).size(), "a JSON object cell value is rejected");
    }

    @Test
    void acceptsScalarCellsInUpdateMatrix() {
        assertTrue(validator.validate(rawTable("ok")).isEmpty());
    }

    @Test
    void rejectsEmptyUpdateMatrix() {
        var view = RawTableView.builder().source(List.of()).build();
        assertEquals(1, validator.validate(view).size(), "an empty source is rejected");
    }

    @Test
    void rejectsNonScalarCellInAppendMatrix() {
        var append = new RawTableAppend();
        append.setRows(List.of(List.of(RawTableCell.builder().value(List.of(1, 2)).build())));
        assertEquals(1, validator.validate(append).size(), "a JSON array cell value is rejected");
    }

    @Test
    void rejectsUnrecognizedHeader() {
        var view = RawTableView.builder()
                .source(List.of(List.of(RawTableCell.builder().value("NotATableType").build())))
                .build();
        assertEquals(1, validator.validate(view).size(), "a header OpenL does not recognize is rejected");
    }

    @Test
    void acceptsRecognizedHeader() {
        var view = RawTableView.builder()
                .source(List.of(List.of(RawTableCell.builder().value("Datatype Greeting").build())))
                .build();
        assertTrue(validator.validate(view).isEmpty(), "a recognized header is accepted");
    }

    @Test
    void acceptsUnrecognizedHeaderForFreeFormTable() {
        var view = RawTableView.builder()
                .kind("Other")
                .source(List.of(List.of(RawTableCell.builder().value("Notes").build())))
                .build();
        assertTrue(validator.validate(view).isEmpty(), "a non-blank free-form header is accepted");
    }

    @Test
    void rejectsBlankHeaderForFreeFormTable() {
        var view = RawTableView.builder()
                .kind("Other")
                .source(List.of(List.of(RawTableCell.builder().value(" ").build())))
                .build();
        assertEquals(1, validator.validate(view).size(), "a free-form table still requires a header");
    }

    @Test
    void acceptsEveryHeaderTheCreateTableModalGenerates() {
        // The exact first cell each table type in the create-table modal produces, paired with the kind it sends.
        // A header the modal can build but the constraint rejects would make that table type impossible to create.
        var headerByKind = Map.ofEntries(
                Map.entry("Datatype NewTable", "Datatype"),
                Map.entry("Datatype NewTable <String>", "Datatype"),
                Map.entry("Constants NewTable", "Constants"),
                Map.entry("Spreadsheet SpreadsheetResult NewTable()", "Spreadsheet"),
                Map.entry("SmartRules Boolean NewTable()", "Rules"),
                Map.entry("SimpleRules Boolean NewTable()", "Rules"),
                Map.entry("SmartLookup Boolean NewTable()", "Rules"),
                Map.entry("SimpleLookup Boolean NewTable()", "Rules"),
                Map.entry("Rules Boolean NewTable()", "Rules"),
                Map.entry("Test Eligibility NewTable", "Test"),
                Map.entry("Run Eligibility NewTable", "Run"),
                Map.entry("Data Customer NewTable", "Data"),
                Map.entry("Environment NewTable", "Environment"),
                Map.entry("Properties NewTable", "Properties"),
                Map.entry("NewTable", "Other"));

        headerByKind.forEach((header, kind) -> {
            var view = RawTableView.builder()
                    .kind(kind)
                    .source(List.of(List.of(RawTableCell.builder().value(header).build())))
                    .build();
            assertTrue(validator.validate(view).isEmpty(), "rejected the generated header: " + header);
        });
    }

    @Test
    void rejectsBlankHeaderCell() {
        // a present top-left cell whose value is null is no recognized header, even with content next to it
        var view = RawTableView.builder()
                .source(List.of(List.of(RawTableCell.builder().value(null).build(),
                        RawTableCell.builder().value("x").build())))
                .build();
        assertEquals(1, validator.validate(view).size(), "a blank header cell is rejected");
    }
}
