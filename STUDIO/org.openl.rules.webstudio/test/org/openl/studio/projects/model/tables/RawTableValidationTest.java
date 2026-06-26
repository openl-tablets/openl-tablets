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
 * append ({@link RawTableAppend}) models, and that an empty matrix is rejected.
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
        return RawTableView.builder()
                .source(List.of(List.of(RawTableCell.builder().value(cellValue).build())))
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
}
