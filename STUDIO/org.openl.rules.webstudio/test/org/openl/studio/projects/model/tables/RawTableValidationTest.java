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
 * append ({@link RawTableAppend}) models, that an empty matrix is rejected, and that a create/update header must
 * name a table type OpenL recognizes.
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
    void rejectsBlankHeaderCell() {
        // a present top-left cell whose value is null is no recognized header, even with content next to it
        var view = RawTableView.builder()
                .source(List.of(List.of(RawTableCell.builder().value(null).build(),
                        RawTableCell.builder().value("x").build())))
                .build();
        assertEquals(1, validator.validate(view).size(), "a blank header cell is rejected");
    }
}
