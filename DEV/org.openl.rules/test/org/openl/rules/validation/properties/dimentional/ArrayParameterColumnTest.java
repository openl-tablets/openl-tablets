package org.openl.rules.validation.properties.dimentional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.java.JavaOpenClass;

class ArrayParameterColumnTest {

    @Test
    void testGetCodeExpression() {
        var arrayColumn = new ArrayParameterColumn(getArrayProperty(), getRules());

        assertEquals("country == null || contains(countryLocal,country)", arrayColumn.getCodeExpression());

        // test array column with one element
        //
        var tableProperty = new TableProperties();
        tableProperty.setCountry(CountriesEnum.CL);
        var properties = new ArrayList<ITableProperties>();
        properties.add(tableProperty);
        var arrayColumn1 = new ArrayParameterColumn(getArrayProperty(),
                new DispatcherTableRules(properties));
        assertEquals("country == null || contains(countryLocal,country)", arrayColumn1.getCodeExpression());

    }

    @Test
    void testGetTitle() {
        var arrayColumn = new ArrayParameterColumn(getArrayProperty(), getRules());

        assertEquals("Countries", arrayColumn.getTitle());
    }

    @Test
    void testParameterDeclaration() {
        var arrayColumn = new ArrayParameterColumn(getArrayProperty(), getRules());

        assertEquals("CountriesEnum[] countryLocal", arrayColumn.getParameterDeclaration());
    }

    @Test
    void testGetMaxNumberOfValuesForRules() {
        var arrayColumn = new ArrayParameterColumn(getArrayProperty(), getRules());

        assertEquals(1, arrayColumn.getNumberOfLocalParameters());
    }

    @Test
    void testGetRuleValue() {
        var arrayColumn = new ArrayParameterColumn(getArrayProperty(), getRules());

        assertEquals(2, arrayColumn.getRulesNumber());
        assertEquals(1, arrayColumn.getNumberOfLocalParameters());
        assertEquals("AT,BA,CL,SA", arrayColumn.getRuleValue(0, 0));
        assertEquals("AU,BE,CA", arrayColumn.getRuleValue(1, 0));
    }

    @Test
    void testNotArrayProperty() {
        // create not array property
        //
        var property = new TablePropertyDefinition();
        property.setType(JavaOpenClass.getOpenClass(String.class));

        try {
            new ArrayParameterColumn(property, getRules());
            fail("Exception should be thrown for not array property");
        } catch (OpenlNotCheckedException e) {
            assertEquals("Cannot create array parameter column for not an array property", e.getMessage());
        }
    }

    private DispatcherTableRules getRules() {
        var tableProperty = new TableProperties();
        tableProperty.setCountry(CountriesEnum.CL, CountriesEnum.BA, CountriesEnum.AT, CountriesEnum.SA);

        var tableProperty1 = new TableProperties();
        tableProperty1.setCountry(CountriesEnum.CA, CountriesEnum.BE, CountriesEnum.AU);
        var properties = new ArrayList<ITableProperties>();
        properties.add(tableProperty);
        properties.add(tableProperty1);

        return new DispatcherTableRules(properties);
    }

    private TablePropertyDefinition getArrayProperty() {
        var arrayProperty = new TablePropertyDefinition();
        arrayProperty.setConstraints(new Constraints("data: countries"));
        arrayProperty.setDescription("Country");
        arrayProperty.setDimensional(true);
        arrayProperty.setDisplayName("Countries");
        arrayProperty.setExpression(
                new MatchingExpression("contains(country)"));
        arrayProperty.setGroup("Business Dimension");
        arrayProperty.setInheritanceLevel(
                new InheritanceLevel[]{InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
        arrayProperty.setName("country");
        arrayProperty.setPrimaryKey(false);
        arrayProperty.setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        arrayProperty.setSystem(false);
        arrayProperty.setType(
                JavaOpenClass.getOpenClass(CountriesEnum[].class));
        return arrayProperty;
    }
}
