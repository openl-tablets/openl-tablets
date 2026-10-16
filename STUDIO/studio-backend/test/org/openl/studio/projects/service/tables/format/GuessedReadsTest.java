package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.dt.DecisionTableColumnHeaders;

/**
 * Covers what the header and the cells of a table the compiler read none of tell: the kinds of the columns of a Rules
 * table, a Vocabulary, the type and the parameters a header names.
 *
 * @author Yury Molchan
 */
class GuessedReadsTest {

    @Test
    void tellsTheKindOfAColumnOfARulesTableInAnyCase() {
        assertEquals(DecisionTableColumnHeaders.CONDITION, GuessedReads.columnKindOf("C1"));
        assertEquals(DecisionTableColumnHeaders.HORIZONTAL_CONDITION, GuessedReads.columnKindOf(" hc2 "));
        assertEquals(DecisionTableColumnHeaders.RETURN, GuessedReads.columnKindOf("RET1"));
        assertEquals(DecisionTableColumnHeaders.COLLECT_RETURN, GuessedReads.columnKindOf("CRET"));
        assertEquals(DecisionTableColumnHeaders.RULE, GuessedReads.columnKindOf("RULE"));
        assertNull(GuessedReads.columnKindOf("Age"), "A title names no kind");
        assertNull(GuessedReads.columnKindOf(null));
        assertFalse(GuessedReads.isColumnKind("C1 x"));
    }

    @Test
    void tellsAFieldNameAndATypeWrittenInAnyScript() {
        assertTrue(GuessedReads.isField("größe"));
        assertTrue(GuessedReads.isField("имя.фамилия[0]"));
        assertTrue(GuessedReads.isField("_res_.$Value$Total (2)"));
        assertFalse(GuessedReads.isField("Größe"), "A title starts with a capital");
        assertTrue(GuessedReads.isType("Größe[]"));
        assertTrue(GuessedReads.isType("Имя"));
        assertFalse(GuessedReads.isType("имя"), "Only a primitive type starts with a lower-case letter");
    }

    @Test
    void tellsAVocabularyByTheTypeOfItsValuesAfterItsName() {
        assertTrue(GuessedReads.isVocabulary("Datatype Gender <String>"));
        assertTrue(GuessedReads.isVocabulary("  Datatype Gender   <String> "));
        assertFalse(GuessedReads.isVocabulary("Datatype Gender<String>"), "The compiler binds a Datatype");
        assertFalse(GuessedReads.isVocabulary("Datatype Person extends Base"));
    }

    @Test
    void tellsTheTypeAndTheParametersAHeaderNames() {
        assertEquals("SpreadsheetResult", GuessedReads.typeOf("Spreadsheet SpreadsheetResult Calc ( Policy p )"));
        assertEquals("void", GuessedReads.typeOf("Spreadsheet void Quiet()"));
        assertEquals("", GuessedReads.typeOf("Spreadsheet Quiet()"));
        assertEquals(2, GuessedReads.parametersOf("SimpleRules Double Rate ( Integer age, String state )"));
        assertEquals(0, GuessedReads.parametersOf("SimpleRules Double Rate ( )"));
    }
}
