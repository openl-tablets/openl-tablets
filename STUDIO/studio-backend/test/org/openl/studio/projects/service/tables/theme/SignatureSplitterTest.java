package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.model.tables.RawTableSignaturePart.KEYWORD;
import static org.openl.studio.projects.model.tables.RawTableSignaturePart.NAME;
import static org.openl.studio.projects.model.tables.RawTableSignaturePart.PARAMETER_NAME;
import static org.openl.studio.projects.model.tables.RawTableSignaturePart.PARAMETER_TYPE;
import static org.openl.studio.projects.model.tables.RawTableSignaturePart.PUNCTUATION;
import static org.openl.studio.projects.model.tables.RawTableSignaturePart.TYPE;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.studio.projects.model.tables.RawTableSignaturePart;

/**
 * Covers how the header text of a table is split into its pieces.
 *
 * @author Yury Molchan
 */
class SignatureSplitterTest {

    @Test
    void splitsARuleIntoItsKeywordTypeNameAndParameters() {
        assertEquals(List.of(piece(KEYWORD, "SmartRules"), piece(TYPE, " Double"), piece(NAME, " Premium"),
                piece(PUNCTUATION, "("), piece(PARAMETER_TYPE, "Driver"), piece(PARAMETER_NAME, " driver"),
                piece(PUNCTUATION, ")")), pieces("SmartRules Double Premium(Driver driver)"));
    }

    @Test
    void takesTheSpacesBeforeAPieceIntoIt() {
        assertEquals(List.of(piece(KEYWORD, "Spreadsheet"), piece(TYPE, " SpreadsheetResult"), piece(NAME, " Plan"),
                piece(PUNCTUATION, "("), piece(PARAMETER_TYPE, " Plan"), piece(PARAMETER_NAME, " plan"),
                piece(PUNCTUATION, " )")), pieces("Spreadsheet SpreadsheetResult Plan( Plan plan )"));
    }

    @Test
    void splitsSeveralParametersByTheirCommas() {
        assertEquals(List.of(piece(KEYWORD, "Rules"), piece(TYPE, " Collect Error[]"), piece(NAME, " Check"),
                piece(PUNCTUATION, " ("), piece(PARAMETER_TYPE, "Map<String, Integer>"),
                piece(PARAMETER_NAME, " limits"), piece(PUNCTUATION, ","), piece(PARAMETER_TYPE, " Policy[]"),
                piece(PARAMETER_NAME, " policies"), piece(PUNCTUATION, ")")),
                pieces("Rules Collect Error[] Check (Map<String, Integer> limits, Policy[] policies)"));
    }

    @Test
    void splitsAHeaderWithoutATypeOrParameters() {
        assertEquals(List.of(piece(KEYWORD, "Spreadsheet"), piece(NAME, " Premium"), piece(PUNCTUATION, "()")),
                pieces("Spreadsheet Premium()"));
        assertEquals(List.of(piece(KEYWORD, "Data"), piece(TYPE, " Policy"), piece(NAME, " policies")),
                pieces("Data Policy policies"));
        assertEquals(List.of(piece(KEYWORD, "Properties"), piece(NAME, " Catalogue  ")), pieces("Properties Catalogue  "));
        assertEquals(List.of(piece(KEYWORD, "Environment")), pieces("Environment"));
        assertEquals(List.of(), pieces("   "));
    }

    @Test
    void splitsADatatypeIntoItsNameAndWhatFollowsIt() {
        assertEquals(List.of(piece(KEYWORD, "Datatype"), piece(NAME, " Code"), piece(TYPE, "<String>")),
                pieces("Datatype Code<String>"));
        assertEquals(List.of(piece(KEYWORD, "Datatype"), piece(NAME, " Child"), piece(TYPE, " extends Parent")),
                pieces("Datatype Child extends Parent"));
    }

    @Test
    void takesAParameterWithoutANameForItsTypeAndReadsWhatFollowsTheParametersAsPunctuation() {
        assertEquals(List.of(piece(KEYWORD, "Method"), piece(TYPE, " void"), piece(NAME, " run"),
                piece(PUNCTUATION, "("), piece(PARAMETER_TYPE, "String"), piece(PUNCTUATION, ") // done")),
                pieces("Method void run(String) // done"));
    }

    /** A piece and the text it covers. */
    private record Text(RawTableSignaturePart part, String text) {
    }

    private static Text piece(RawTableSignaturePart part, String text) {
        return new Text(part, text);
    }

    /** The pieces of a header, each with the text it covers. */
    private static List<Text> pieces(String header) {
        var pieces = new ArrayList<Text>();
        var from = 0;
        for (var piece : SignatureSplitter.split(header)) {
            pieces.add(new Text(piece.part(), header.substring(from, piece.to())));
            from = piece.to();
        }
        return pieces;
    }
}
