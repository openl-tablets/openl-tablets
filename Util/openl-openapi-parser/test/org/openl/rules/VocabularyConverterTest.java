package org.openl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.VocabularyModel;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;

/**
 * The enums of a specification become vocabulary datatypes, and every place that lists the same values names the
 * same vocabulary.
 */
class VocabularyConverterTest {

    private static final TypeInfo KIND = new TypeInfo("java.lang.String", "Kind", TypeInfo.Type.VOCABULARY);

    @Test
    void declaresAVocabularyPerDistinctEnum() {
        var projectModel = extract();

        assertEquals(List.of(new VocabularyModel("Color", "String", List.of("red", "green")),
                new VocabularyModel("Kind", "String", List.of("bla1", "bla2", "bla3")),
                new VocabularyModel("Level", "Integer", List.of("1", "2", "3"))), projectModel.getVocabularyModels());
        assertEquals(List.of("Bean"), projectModel.getDatatypeModels().stream().map(DatatypeModel::getName).toList());
    }

    @Test
    void typesTheFieldsByTheVocabularies() {
        var bean = extract().getDatatypeModels().iterator().next();

        assertEquals(List.of("Kind kind", "Kind[] kinds", "String note", "Color color", "Level level", "Color[] colors"),
                bean.getFields().stream().map(f -> f.getType() + " " + f.getName()).toList());
    }

    @Test
    void passesTheVocabulariesThroughTheSpreadsheets() {
        var projectModel = extract();

        var calc = spreadsheet(projectModel, "calc");
        assertEquals(KIND, calc.getParameters().getFirst().getType());
        assertEquals(List.of(new StepModel("Kind", "Kind", "= (Kind) null"),
                new StepModel("Kinds", "Kind[]", "= (Kind[]) null"),
                new StepModel("Note", "String", "= \"\"")), calc.getSteps());

        var r4 = spreadsheet(projectModel, "r4");
        assertEquals("Kind", r4.getType());
        assertEquals(List.of(new StepModel("Result", "Kind", "= (Kind) null")), r4.getSteps());

        assertEquals(KIND, spreadsheet(projectModel, "r1").getParameters().getFirst().getType());
        var kinds = spreadsheet(projectModel, "r2").getParameters().getFirst().getType();
        assertEquals(new TypeInfo("[Ljava.lang.String;", "Kind[]", TypeInfo.Type.VOCABULARY, 1, false), kinds);
    }

    /** A named schema keeps its name even when another one lists the same values. */
    @Test
    void keepsTheNameOfEveryNamedSchema() {
        var projectModel = extract("vocabulary-references");

        assertEquals(List.of("Currency", "BaseCurrency", "Nullable", "Blanks", "Status", "Level", "Grade", "Kind", "QResult", "B"),
                projectModel.getVocabularyModels().stream().map(VocabularyModel::getName).toList());
        var bean = projectModel.getDatatypeModels().iterator().next();
        assertEquals(List.of("Currency c", "BaseCurrency b", "Nullable n", "Status status", "Level level", "Blanks blank", "Grade grade"),
                bean.getFields().stream().map(f -> f.getType() + " " + f.getName()).toList());
    }

    /** A null or an empty value has no row in a vocabulary table, so it is left out. */
    @Test
    void leavesOutTheValuesATableCannotHold() {
        var vocabularies = extract("vocabulary-references").getVocabularyModels();

        assertEquals(List.of("A", "B"), vocabularies.get(2).values());
        assertEquals(List.of("X"), vocabularies.get(3).values());
    }

    /**
     * The default of a field stays with the field when its type becomes a vocabulary; a vocabulary field without
     * one starts unset, not at the zero a plain integer starts at.
     */
    @Test
    void keepsTheDefaultOfAVocabularyField() {
        var bean = extract("vocabulary-references").getDatatypeModels().iterator().next();

        assertEquals("ACTIVE", bean.getFields().get(3).getDefaultValue());
        assertEquals(2, bean.getFields().get(4).getDefaultValue());
        assertNull(bean.getFields().get(6).getDefaultValue());
    }

    /** A reference to a parameter or a response the specification does not declare is passed over. */
    @Test
    void passesOverDanglingReferences() {
        var dangling = spreadsheet(extract("vocabulary-references"), "dangling");

        assertEquals("String", dangling.getType());
    }

    /** An enum behind a reference to a component parameter, request body or response is a vocabulary too. */
    @Test
    void namesTheEnumsBehindComponentReferences() {
        var projectModel = extract("vocabulary-references");

        assertEquals("Kind", spreadsheet(projectModel, "q").getParameters().getFirst().getType().getSimpleName());
        assertEquals("QResult", spreadsheet(projectModel, "q").getType());
        assertEquals("B", spreadsheet(projectModel, "b").getParameters().getFirst().getType().getSimpleName());
    }

    private static ProjectModel extract() {
        return extract("vocabulary");
    }

    private static ProjectModel extract(String specification) {
        return new OpenAPIScaffoldingConverter().extractProjectModel("test.converter/datatype/" + specification + ".json");
    }

    private static SpreadsheetModel spreadsheet(ProjectModel projectModel, String name) {
        return projectModel.getSpreadsheetResultModels()
                .stream()
                .filter(spreadsheet -> spreadsheet.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
