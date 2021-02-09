package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;

public class ProjectModelTest {

    public static final String BANK_RATING = "BankRating";
    public static final String INSURANCE_POLICY = "InsurancePolicy";

    @Test
    public void testProjectModelCreation() {
        ProjectModel bankRating = new ProjectModel();
        bankRating.setName(BANK_RATING);
        ProjectModel bankRatingCopy = new ProjectModel();
        bankRatingCopy.setName(BANK_RATING);
        ProjectModel insurancePolicy = new ProjectModel();
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
    public void testProjectModelWithContext() {
        ProjectModel bankRating = new ProjectModel(BANK_RATING,
            true,
            false,
            Collections.emptySet(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());
        ProjectModel bankRatingCopy = new ProjectModel(BANK_RATING,
            true,
            false,
            Collections.emptySet(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());
        ProjectModel bankRatingWithoutContext = new ProjectModel(BANK_RATING,
            false,
            false,
            Collections.emptySet(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());
        assertEquals(bankRating, bankRatingCopy);
        assertEquals(bankRating.hashCode(), bankRatingCopy.hashCode());
        assertTrue(bankRating.isRuntimeContextProvided());
        assertFalse(bankRatingWithoutContext.isRuntimeContextProvided());
        assertNotEquals(bankRating, bankRatingWithoutContext);
        assertNotEquals(bankRating.hashCode(), bankRatingWithoutContext.hashCode());
    }

    @Test
    public void testProjectModelWithSpreadsheets() {
        SpreadsheetModel firstSpr = new SpreadsheetModel();
        firstSpr.setName("getBankAccountDetails");
        SpreadsheetModel secondSpr = new SpreadsheetModel();
        secondSpr.setName("getBankAccountData");

        ProjectModel bankRating = new ProjectModel(BANK_RATING,
            true,
            false,
            Collections.emptySet(),
            Collections.emptyList(),
            Arrays.asList(firstSpr, secondSpr),
            Collections.emptyList());
        ProjectModel bankRatingCopy = new ProjectModel(BANK_RATING,
            true,
            false,
            Collections.emptySet(),
            Collections.emptyList(),
            Arrays.asList(firstSpr, secondSpr),
            Collections.emptyList());
        ProjectModel bankRatingWithOneSpr = new ProjectModel(BANK_RATING,
            true,
            false,
            Collections.emptySet(),
            Collections.emptyList(),
            Collections.singletonList(firstSpr),
            Collections.emptyList());

        assertEquals(bankRating, bankRatingCopy);
        assertEquals(bankRating.hashCode(), bankRatingCopy.hashCode());
        assertEquals(bankRating, bankRating);
        assertNotEquals(bankRating, bankRatingWithOneSpr);
        assertNotEquals(bankRating.hashCode(), bankRatingWithOneSpr.hashCode());
        assertEquals(2, bankRating.getSpreadsheetResultModels().size());
        assertEquals(1, bankRatingWithOneSpr.getSpreadsheetResultModels().size());
    }

    @Test
    public void testProjectModelWithDataTypes() {
        DatatypeModel dm = new DatatypeModel("Apple");
        DatatypeModel oneMoreDm = new DatatypeModel("Meat");
        ProjectModel bankRating = new ProjectModel(BANK_RATING,
            true,
            false,
            asSet(dm, oneMoreDm),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());
        ProjectModel bankRatingCopy = new ProjectModel(BANK_RATING,
            true,
            false,
            asSet(dm, oneMoreDm),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());
        ProjectModel bankRatingWithOneDataType = new ProjectModel(BANK_RATING,
            true,
            false,
            asSet(oneMoreDm),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

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
