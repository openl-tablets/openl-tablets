package org.open.rules.model.scaffolding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;

class ProjectModelTest {

    private static final String BANK_RATING = "BankRating";
    private static final String INSURANCE_POLICY = "InsurancePolicy";

    @Test
    void testProjectModelCreation() {
        var bankRating = new ProjectModel();
        bankRating.setName(BANK_RATING);
        var bankRatingCopy = new ProjectModel();
        bankRatingCopy.setName(BANK_RATING);
        var insurancePolicy = new ProjectModel();
        insurancePolicy.setName(INSURANCE_POLICY);

        assertEquals(bankRating, bankRating);
        assertEquals(bankRating, bankRatingCopy);
        assertEquals(bankRating.hashCode(), bankRatingCopy.hashCode());
        assertNotEquals(bankRating, null);
        assertNotEquals(bankRating, insurancePolicy);
        assertNotEquals(bankRating.hashCode(), insurancePolicy.hashCode());
        assertEquals(BANK_RATING, bankRating.getName());
        assertEquals(INSURANCE_POLICY, insurancePolicy.getName());
    }

    @Test
    void testProjectModelWithContext() {
        var bankRating = new ProjectModel(BANK_RATING,
                true,
                Set.of(),
                List.of(),
                List.of(),
                List.of());
        var bankRatingCopy = new ProjectModel(BANK_RATING,
                true,
                Set.of(),
                List.of(),
                List.of(),
                List.of());
        var bankRatingWithoutContext = new ProjectModel(BANK_RATING,
                false,
                Set.of(),
                List.of(),
                List.of(),
                List.of());
        assertEquals(bankRating, bankRatingCopy);
        assertEquals(bankRating.hashCode(), bankRatingCopy.hashCode());
        assertTrue(bankRating.isRuntimeContextProvided());
        assertFalse(bankRatingWithoutContext.isRuntimeContextProvided());
        assertNotEquals(bankRating, bankRatingWithoutContext);
        assertNotEquals(bankRating.hashCode(), bankRatingWithoutContext.hashCode());
    }

    @Test
    void testProjectModelWithSpreadsheets() {
        var firstSpr = new SpreadsheetModel();
        firstSpr.setName("getBankAccountDetails");
        var secondSpr = new SpreadsheetModel();
        secondSpr.setName("getBankAccountData");

        var bankRating = new ProjectModel(BANK_RATING,
                true,
                Set.of(),
                List.of(),
                Arrays.asList(firstSpr, secondSpr),
                List.of());
        var bankRatingCopy = new ProjectModel(BANK_RATING,
                true,
                Set.of(),
                List.of(),
                Arrays.asList(firstSpr, secondSpr),
                List.of());
        var bankRatingWithOneSpr = new ProjectModel(BANK_RATING,
                true,
                Set.of(),
                List.of(),
                List.of(firstSpr),
                List.of());

        assertEquals(bankRating, bankRatingCopy);
        assertEquals(bankRating.hashCode(), bankRatingCopy.hashCode());
        assertEquals(bankRating, bankRating);
        assertNotEquals(bankRating, bankRatingWithOneSpr);
        assertNotEquals(bankRating.hashCode(), bankRatingWithOneSpr.hashCode());
        assertEquals(2, bankRating.getSpreadsheetResultModels().size());
        assertEquals(1, bankRatingWithOneSpr.getSpreadsheetResultModels().size());
    }

    @Test
    void testProjectModelWithDataTypes() {
        var dm = new DatatypeModel("Apple");
        var oneMoreDm = new DatatypeModel("Meat");
        var bankRating = new ProjectModel(BANK_RATING,
                true,
                asSet(dm, oneMoreDm),
                List.of(),
                List.of(),
                List.of());
        var bankRatingCopy = new ProjectModel(BANK_RATING,
                true,
                asSet(dm, oneMoreDm),
                List.of(),
                List.of(),
                List.of());
        var bankRatingWithOneDataType = new ProjectModel(BANK_RATING,
                true,
                asSet(oneMoreDm),
                List.of(),
                List.of(),
                List.of());

        assertEquals(bankRating, bankRatingCopy);
        assertEquals(bankRating.hashCode(), bankRatingCopy.hashCode());
        assertEquals(bankRating, bankRating);
        assertNotEquals(bankRating, bankRatingWithOneDataType);
        assertNotEquals(bankRating.hashCode(), bankRatingWithOneDataType.hashCode());
        assertEquals(2, bankRating.getDatatypeModels().size());
        assertEquals(1, bankRatingWithOneDataType.getDatatypeModels().size());
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... args) {
        return new LinkedHashSet<>(Arrays.asList(args));
    }
}
