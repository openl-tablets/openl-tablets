package org.openl.rules.validator.dt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.domain.DateRangeDomain;
import org.openl.domain.EnumDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.domain.StringDomain;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.dt.type.domains.DateRangeDomainAdaptor;
import org.openl.rules.dt.type.domains.EnumDomainAdaptor;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.type.domains.IntRangeDomainAdaptor;
import org.openl.rules.dt.validator.DecisionTableOverlapping;
import org.openl.rules.dt.validator.DecisionTableUncovered;
import org.openl.rules.dt.validator.DecisionTableValidationResult;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

public class ValidatorTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/Test_Validator_DT.xls";

    public ValidatorTest() {
        super(SRC);
    }

    @Test
    public void testOk() {
        String tableName = "Rules String validationOK(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        Map<String, IDomainAdaptor> domains = new HashMap<>();

        // EnumDomain<TestValidationEnum1> enumDomain1 = new
        // EnumDomain<TestValidationEnum1>(new
        // TestValidationEnum1[]{TestValidationEnum1.V1,
        // TestValidationEnum1.V2});
        // EnumDomainAdaptor enumDomainAdaptor1 = new
        // EnumDomainAdaptor(enumDomain1);
        // domains.put("value1", enumDomainAdaptor1);
        //
        // EnumDomain<TestValidationEnum2> enumDomain2 = new
        // EnumDomain<TestValidationEnum2>(new
        // TestValidationEnum2[]{TestValidationEnum2.V1,
        // TestValidationEnum2.V2});
        // EnumDomainAdaptor enumDomainAdaptor2 = new
        // EnumDomainAdaptor(enumDomain2);
        // domains.put("value2", enumDomainAdaptor2);

        DecisionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    }

    @Test
    public void testGap() {
        String tableName = "Rules String validationGap(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        DecisionTableValidationResult dtValidResult = testTable(tableName, null);
        assertEquals(1, dtValidResult.getUncovered().length);
    }

    @Test
    public void testOverlap() {
        String tableName = "Rules String validationOverlap(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        DecisionTableValidationResult dtValidResult = testTable(tableName, null);
        assertEquals(1, dtValidResult.getOverlappings().length);
    }

    @Test
    public void testIntRule() {
        String tableName = "Rules void hello1(int hour)";
        IntRangeDomain intRangeDomain = new IntRangeDomain(0, 24);
        Map<String, IDomainAdaptor> domains = new HashMap<>();
        IntRangeDomainAdaptor intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("hour", intRangeDomainAdaptor);

        DecisionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertEquals(1, dtValidResult.getUncovered().length);
        assertEquals("Param value missing", "hour = 24", dtValidResult.getUncovered()[0].getValues().toString());
    }

    @SuppressWarnings("deprecation")
    private DecisionTableValidationResult testTable(String tableName, Map<String, IDomainAdaptor> domains) {
        DecisionTableValidationResult result = null;
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {
            ITableProperties tableProperties = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
            assertTrue(getCompiledOpenClass().getBindingErrors().length == 0);
            assertTrue(getCompiledOpenClass().getParsingErrors().length == 0);

            IDecisionTable dt = (IDecisionTable) resultTsn.getMember();
            try {
                // System.out.println("Validating <" + tableName+ ">");
                result = DecisionTableValidator.validateTable(dt, domains, getCompiledOpenClass().getOpenClass());

                if (result.hasProblems()) {
                    resultTsn.setValidationResult(result);
                    // System.out.println("There are problems in table!!\n");
                } else {
                    // System.out.println("NO PROBLEMS IN TABLE!!!!\n");
                }
            } catch (Exception t) {
                fail();
            }
        } else {
            fail();
        }
        return result;
    }

    @Test
    public void testOk2() {
        String tableName = "Rules void hello2(int currentValue)";
        IntRangeDomain intRangeDomain = new IntRangeDomain(0, 50);
        Map<String, IDomainAdaptor> domains = new HashMap<>();
        IntRangeDomainAdaptor intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("currentValue", intRangeDomainAdaptor);

        DecisionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    }

    @Test
    public void testString() {
        String tableName = "Rules void helloString(String stringValue)";
        Map<String, IDomainAdaptor> domains = new HashMap<>();
        StringDomain stringDomain = new StringDomain(new String[] { "value1", "value2", "value3" });
        EnumDomainAdaptor enumDomainStrAdaptor = new EnumDomainAdaptor(stringDomain);

        domains.put("stringValue", enumDomainStrAdaptor);
        domains.put("localValue", enumDomainStrAdaptor);

        DecisionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertTrue(dtValidResult.hasProblems());
    }

    @Test
    public void testDate() {
        String tableName = "Rules void testDate(Date currentDate)";
        Map<String, IDomainAdaptor> domains = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        Date startDate = null;
        Date endDate = null;
        try {
            startDate = dateFormat.parse("01/01/1900");
            endDate = dateFormat.parse("01/01/2050");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        DateRangeDomain dateRangeDomain = new DateRangeDomain(startDate, endDate);
        DateRangeDomainAdaptor adaptor = new DateRangeDomainAdaptor(dateRangeDomain);

        domains.put("currentDate", adaptor);
        domains.put("min", adaptor);
        domains.put("max", adaptor);

        DecisionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertTrue(!dtValidResult.hasProblems());

        Date newEndDate = null;
        try {
            newEndDate = dateFormat.parse("01/01/2150");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        dateRangeDomain.setMax(newEndDate);
        dtValidResult = testTable(tableName, domains);
        assertTrue(dtValidResult.getUncovered().length == 1);
    }

    @Test
    public void testArrayContains() {
        String tableName = "Rules void testArrayContains(TestValidationEnum3 value)";

        DecisionTableValidationResult dtValidResult = testTable(tableName, null);
        assertFalse(dtValidResult.hasProblems());
    }

    @Test
    public void testArrayContainsOverlap() {
        String tableName = "Rules void testArrayContainsOverlap(TestValidationEnum3 value)";

        DecisionTableValidationResult dtValidResult = testTable(tableName, null);
        assertFalse(dtValidResult.hasProblems());
        assertTrue(dtValidResult.getOverlappings().length == 1);
        DecisionTableOverlapping overlap = dtValidResult.getOverlappings()[0];
        assertEquals("value = V2", overlap.getValues().toString());
    }

    @Test
    public void testArrayContainsGap() {
        String tableName = "Rules void testArrayContainsGap(TestValidationEnum3 value)";

        DecisionTableValidationResult dtValidResult = testTable(tableName, null);
        assertTrue(dtValidResult.hasProblems());
        assertTrue(dtValidResult.getUncovered().length == 1);
        DecisionTableUncovered gap = dtValidResult.getUncovered()[0];
        assertEquals("value = V4", gap.getValues().toString());
    }

    @Test
    public void testCountries() {
        // test narrowed domain for enum values.
        String tableName = "Rules void testCountries(CountriesEnum country)";
        Map<String, IDomainAdaptor> domains = new HashMap<>();

        EnumDomain<CountriesEnum> enumDomain1 = new EnumDomain<>(
            new CountriesEnum[] { CountriesEnum.AR, CountriesEnum.AU, CountriesEnum.BR, CountriesEnum.CA });
        EnumDomainAdaptor enumDomainAdaptor1 = new EnumDomainAdaptor(enumDomain1);
        domains.put("country", enumDomainAdaptor1);
        domains.put("countryLocal1", enumDomainAdaptor1);
        domains.put("countryLocal2", enumDomainAdaptor1);
        domains.put("countryLocal3", enumDomainAdaptor1);
        domains.put("countryLocal4", enumDomainAdaptor1);
        domains.put("countryLocal5", enumDomainAdaptor1);
        domains.put("countryLocal6", enumDomainAdaptor1);
        domains.put("countryLocal7", enumDomainAdaptor1);
        domains.put("countryLocal8", enumDomainAdaptor1);
        domains.put("countryLocal9", enumDomainAdaptor1);
        domains.put("countryLocal10", enumDomainAdaptor1);
        domains.put("countryLocal11", enumDomainAdaptor1);
        domains.put("countryLocal12", enumDomainAdaptor1);
        domains.put("countryLocal13", enumDomainAdaptor1);
        domains.put("countryLocal14", enumDomainAdaptor1);
        domains.put("countryLocal15", enumDomainAdaptor1);
        domains.put("countryLocal16", enumDomainAdaptor1);

        DecisionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    }

}
