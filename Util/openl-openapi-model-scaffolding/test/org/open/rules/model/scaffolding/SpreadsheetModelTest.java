package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;

public class SpreadsheetModelTest {

    public static final String SPR_NAME = "getBankAccountNumber";
    public static final String STRING = "String";

    @Test
    public void testSpreadsheetModelCreation() {
        SpreadsheetModel first = new SpreadsheetModel();
        first.setName(SPR_NAME);
        assertEquals(SPR_NAME, first.getName());
        SpreadsheetModel second = new SpreadsheetModel();
        second.setName(SPR_NAME);
        assertEquals(SPR_NAME, second.getName());
        SpreadsheetModel third = new SpreadsheetModel();
        third.setName("getBankAccountDetails");

        assertEquals(first, second);
        assertNotEquals(first, null);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
        assertNotEquals(first.hashCode(), third.hashCode());
        assertNotEquals(second, null);

        first.setType(STRING);
        assertEquals(STRING, first.getType());
        second.setType(STRING);
        assertEquals(STRING, second.getType());

        third.setName(SPR_NAME);
        third.setType("Double");
        assertEquals("Double", third.getType());

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
        assertNotEquals(first.hashCode(), third.hashCode());
    }

    @Test
    public void testSprWithParameters() {
        InputParameter firstName = new ParameterModel(new TypeInfo(String.class), "firstName");
        InputParameter secondName = new ParameterModel(new TypeInfo(String.class), "secondName");
        InputParameter city = new ParameterModel(new TypeInfo(String.class), "city");
        List<InputParameter> nameParams = Arrays.asList(firstName, secondName);
        List<InputParameter> fullParamsList = Arrays.asList(firstName, secondName, city);

        SpreadsheetModel first = new SpreadsheetModel();
        first.setName(SPR_NAME);
        first.setType(STRING);
        first.setParameters(nameParams);

        SpreadsheetModel second = new SpreadsheetModel();
        second.setName(SPR_NAME);
        second.setType(STRING);
        second.setParameters(nameParams);

        SpreadsheetModel third = new SpreadsheetModel();
        third.setName(SPR_NAME);
        third.setType(STRING);
        third.setParameters(fullParamsList);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
        assertNotEquals(first.hashCode(), third.hashCode());

        assertEquals(2, first.getParameters().size());
        assertEquals(3, third.getParameters().size());
    }

    @Test
    public void testSprWithStepsAndParams() {
        StepModel firstName = new StepModel("String", "firstName", "First name of person.", "John");
        StepModel secondName = new StepModel("secondName", "String", "Second name of person.", "Doe");
        StepModel city = new StepModel("city", "String", "The place of birth.", "Kazan");
        List<StepModel> steps = Arrays.asList(firstName, secondName);
        List<StepModel> fullSteps = Arrays.asList(firstName, secondName, city);

        InputParameter date = new ParameterModel(new TypeInfo(Date.class), "dateOfBirth");
        InputParameter count = new ParameterModel(new TypeInfo(Integer.class), "count");
        List<InputParameter> params = Arrays.asList(date, count);

        SpreadsheetModel first = new SpreadsheetModel();
        first.setName(SPR_NAME);
        first.setType(STRING);
        first.setSteps(steps);

        SpreadsheetModel second = new SpreadsheetModel();
        second.setName(SPR_NAME);
        second.setType(STRING);
        second.setSteps(steps);

        SpreadsheetModel third = new SpreadsheetModel();
        third.setName(SPR_NAME);
        third.setType(STRING);
        third.setSteps(fullSteps);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
        assertNotEquals(first.hashCode(), third.hashCode());
        assertEquals(2, first.getSteps().size());
        assertEquals(3, third.getSteps().size());

        first.setParameters(params);
        second.setParameters(params);
        third.setParameters(params);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
        assertNotEquals(first.hashCode(), third.hashCode());

        third.setParameters(Collections.singletonList(date));
        assertNotEquals(first, third);
        assertNotEquals(first.hashCode(), third.hashCode());

        assertEquals(first, first);
    }

    @Test
    public void testSprWithPathInfo() {
        PathInfo xyzInfo = new PathInfo("/xyz",
            "xyz",
            PathInfo.Operation.GET,
            new TypeInfo(String.class),
            "application/json",
            "text/plain");
        PathInfo xyInfo = new PathInfo("/xyz/xy/{far}",
            "xyzxy",
            PathInfo.Operation.GET,
            new TypeInfo(String.class),
            "application/json",
            "text/plain");

        SpreadsheetModel spr = new SpreadsheetModel();
        spr.setName(SPR_NAME);
        spr.setType(STRING);
        spr.setSteps(Collections.emptyList());
        spr.setPathInfo(xyzInfo);

        SpreadsheetModel secondSpr = new SpreadsheetModel();
        secondSpr.setName(SPR_NAME);
        secondSpr.setType(STRING);
        secondSpr.setSteps(Collections.emptyList());
        secondSpr.setPathInfo(xyzInfo);

        assertEquals(spr, secondSpr);
        assertEquals(spr.hashCode(), secondSpr.hashCode());
        assertEquals(spr.getPathInfo(), secondSpr.getPathInfo());

        SpreadsheetModel thirdSpr = new SpreadsheetModel();
        secondSpr.setName(SPR_NAME);
        secondSpr.setType(STRING);
        secondSpr.setSteps(Collections.emptyList());
        secondSpr.setPathInfo(xyInfo);

        assertNotEquals(spr, thirdSpr);
        assertNotEquals(spr.hashCode(), thirdSpr.hashCode());
        assertNotEquals(spr.getPathInfo(), thirdSpr.getPathInfo());
    }

}
