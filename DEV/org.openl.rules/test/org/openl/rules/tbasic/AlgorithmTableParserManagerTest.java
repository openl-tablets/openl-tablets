package org.openl.rules.tbasic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.openl.rules.tbasic.compile.ConversionRuleBean;

public class AlgorithmTableParserManagerTest {

    @Test
    public final void testGetAlgorithmSpecification() {
        TableParserSpecificationBean[] specifications = AlgorithmTableParserManager.getInstance()
            .getAlgorithmSpecification();
        assertNotNull(specifications);

        TableParserSpecificationBean[] theSameSpecifications = AlgorithmTableParserManager.getInstance()
            .getAlgorithmSpecification();
        assertNotNull(theSameSpecifications);
        assertArrayEquals(specifications, theSameSpecifications);
    }

    @Test
    public final void testGetConversionRules() {
        ConversionRuleBean[] conversionRules = AlgorithmTableParserManager.getInstance().getConversionRules();

        assertNotNull(conversionRules);

        ConversionRuleBean[] theSameConversionRules = AlgorithmTableParserManager.getInstance().getConversionRules();

        assertNotNull(theSameConversionRules);
        assertArrayEquals(conversionRules, theSameConversionRules);
    }

    @Test
    public final void testGetFixedConversionRules() {
        ConversionRuleBean[] conversionRules = AlgorithmTableParserManager.getInstance().getFixedConversionRules();

        assertNotNull(conversionRules);

        ConversionRuleBean[] theSameConversionRules = AlgorithmTableParserManager.getInstance()
            .getFixedConversionRules();

        assertNotNull(theSameConversionRules);
        assertArrayEquals(conversionRules, theSameConversionRules);
    }

    @Test
    public final void testInstance() {
        assertNotNull(AlgorithmTableParserManager.getInstance());
    }

    @Test
    public final void testWhatIsOperationsGroupName() {
        AlgorithmTableParserManager parserManager = AlgorithmTableParserManager.getInstance();

        List<String> operationsGroup1 = Arrays.asList(new String[] { "IF" });
        assertEquals("IF", parserManager.whatIsOperationsGroupName(operationsGroup1));

        List<String> operationsGroup2 = Arrays.asList(new String[] { "IF", "ELSE" });
        assertEquals("IFELSE", parserManager.whatIsOperationsGroupName(operationsGroup2));

        List<String> operationsGroup3 = Arrays.asList(new String[] { "IF", "ELSE", "END IF" });
        assertEquals("IFELSE", parserManager.whatIsOperationsGroupName(operationsGroup3));

        List<String> operationsGroup4 = Arrays.asList(new String[] { "IF", "END IF" });
        assertEquals("IF", parserManager.whatIsOperationsGroupName(operationsGroup4));

        List<String> operationsGroup5 = Arrays.asList(new String[] { "WHILE" });
        assertEquals("WHILE", parserManager.whatIsOperationsGroupName(operationsGroup5));

        List<String> operationsGroup6 = Arrays.asList(new String[] { "WHILE", "END WHILE" });
        assertEquals("WHILE", parserManager.whatIsOperationsGroupName(operationsGroup6));

        List<String> operationsGroup7 = Arrays.asList(new String[] { "VAR" });
        assertEquals("VAR", parserManager.whatIsOperationsGroupName(operationsGroup7));

    }

    @Test
    public final void testWhatOperationsToGroup() {
        String[] operationNames1 = AlgorithmTableParserManager.getInstance().whatOperationsToGroup("IF");
        assertArrayEquals(new String[] { "ELSE", "END IF" }, operationNames1);

        String[] operationNames2 = AlgorithmTableParserManager.getInstance().whatOperationsToGroup("VAR");
        assertNull(operationNames2);
    }

}
