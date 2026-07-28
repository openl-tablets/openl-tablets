package org.openl.rules.validator.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.domain.DateRangeDomain;
import org.openl.domain.EnumDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.dt.type.domains.DateRangeDomainAdaptor;
import org.openl.rules.dt.type.domains.EnumDomainAdaptor;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.type.domains.IntRangeDomainAdaptor;
import org.openl.rules.dt.validator.DecisionTableValidationResult;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.enumeration.CountriesEnum;

class ValidatorTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/Test_Validator_DT.xls";

    public ValidatorTest() {
        super(SRC);
    }

    @Test
    void testOk() {
        var tableName = "Rules String validationOK(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        var domains = new HashMap<String, IDomainAdaptor>();

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

        var dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    }

    @Test
    void testGap() {
        var tableName = "Rules String validationGap(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        var dtValidResult = testTable(tableName, null);
        assertEquals(1, dtValidResult.getUncovered().length);
    }

    @Test
    void testOverlap() {
        var tableName = "Rules String validationOverlap(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        var dtValidResult = testTable(tableName, null);
        assertEquals(1, dtValidResult.getOverlappings().length);
    }

    @Test
    void testIntRule() {
        var tableName = "Rules void hello1(int hour)";
        var intRangeDomain = new IntRangeDomain(0, 24);
        var domains = new HashMap<String, IDomainAdaptor>();
        var intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("hour", intRangeDomainAdaptor);

        var dtValidResult = testTable(tableName, domains);
        assertEquals(1, dtValidResult.getUncovered().length);
        assertEquals("hour = 24", dtValidResult.getUncovered()[0].getValues().toString(), "Param value missing");
    }

    @SuppressWarnings("deprecation")
    private DecisionTableValidationResult testTable(String tableName, Map<String, IDomainAdaptor> domains) {
        DecisionTableValidationResult result = null;
        var resultTsn = findTable(tableName);
        if (resultTsn != null) {
            var tableProperties = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
            assertFalse(getCompiledOpenClass().hasErrors());

            var dt = (IDecisionTable) resultTsn.getMember();
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
    void testOk2() {
        var tableName = "Rules void hello2(int currentValue)";
        var intRangeDomain = new IntRangeDomain(0, 50);
        var domains = new HashMap<String, IDomainAdaptor>();
        var intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("currentValue", intRangeDomainAdaptor);

        var dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    }

    @Test
    void testString() {
        var tableName = "Rules void helloString(String stringValue)";
        var domains = new HashMap<String, IDomainAdaptor>();
        var stringDomain = new EnumDomain<String>(new String[]{"value1", "value2", "value3"});
        var enumDomainStrAdaptor = new EnumDomainAdaptor(stringDomain);

        domains.put("stringValue", enumDomainStrAdaptor);
        domains.put("localValue", enumDomainStrAdaptor);

        var dtValidResult = testTable(tableName, domains);
        assertTrue(dtValidResult.hasProblems());
    }

    @Test
    void testDate() {
        var tableName = "Rules void testDate(Date currentDate)";
        var domains = new HashMap<String, IDomainAdaptor>();
        var dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        Date startDate = null;
        Date endDate = null;
        try {
            startDate = dateFormat.parse("01/01/1900");
            endDate = dateFormat.parse("01/01/2050");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        var dateRangeDomain = new DateRangeDomain(startDate, endDate);
        var adaptor = new DateRangeDomainAdaptor(dateRangeDomain);

        domains.put("currentDate", adaptor);
        domains.put("min", adaptor);
        domains.put("max", adaptor);

        var dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());

        Date newEndDate = null;
        try {
            newEndDate = dateFormat.parse("01/01/2150");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        dateRangeDomain.setMax(newEndDate);
        dtValidResult = testTable(tableName, domains);
        assertEquals(1, dtValidResult.getUncovered().length);
    }

    @Test
    void testArrayContains() {
        var tableName = "Rules void testArrayContains(TestValidationEnum3 value)";

        var dtValidResult = testTable(tableName, null);
        assertFalse(dtValidResult.hasProblems());
    }

    @Test
    void testArrayContainsOverlap() {
        var tableName = "Rules void testArrayContainsOverlap(TestValidationEnum3 value)";

        var dtValidResult = testTable(tableName, null);
        assertFalse(dtValidResult.hasProblems());
        assertEquals(1, dtValidResult.getOverlappings().length);
        var overlap = dtValidResult.getOverlappings()[0];
        assertEquals("value = V2", overlap.getValues().toString());
    }

    @Test
    void testArrayContainsGap() {
        var tableName = "Rules void testArrayContainsGap(TestValidationEnum3 value)";

        var dtValidResult = testTable(tableName, null);
        assertTrue(dtValidResult.hasProblems());
        assertEquals(1, dtValidResult.getUncovered().length);
        var gap = dtValidResult.getUncovered()[0];
        assertEquals("value = V4", gap.getValues().toString());
    }

    @Test
    void testCountries() {
        // test narrowed domain for enum values.
        var tableName = "Rules void testCountries(CountriesEnum country)";
        var domains = new HashMap<String, IDomainAdaptor>();

        var enumDomain1 = new EnumDomain<CountriesEnum>(
                new CountriesEnum[]{CountriesEnum.AR, CountriesEnum.AU, CountriesEnum.BR, CountriesEnum.CA});
        var enumDomainAdaptor1 = new EnumDomainAdaptor(enumDomain1);
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

        var dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    }

}
