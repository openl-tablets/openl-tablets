package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.OriginsEnum;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.table.properties.ITableProperties;

class TablePropertiesImplTest {

    @Test
    void readsTheCurrentPropertiesOfTheRule() {
        var tableProperties = new org.openl.rules.table.properties.TableProperties();
        TableProperties properties = new TablePropertiesImpl(tableProperties);
        tableProperties.setState(UsStatesEnum.CA);
        tableProperties.setNature("Premium");

        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.CA}, properties.getState());
        assertEquals("Premium", properties.getNature());
    }

    @Test
    void readsEveryPropertyAsTheRuleHasIt() throws Exception {
        var tableProperties = new org.openl.rules.table.properties.TableProperties();
        tableProperties.setName("rate");
        tableProperties.setLob("Auto", "Home");
        tableProperties.setActive(Boolean.TRUE);
        tableProperties.setPriority(3);
        var properties = new TablePropertiesImpl(tableProperties);

        for (var getter : TableProperties.class.getMethods()) {
            var expected = ITableProperties.class.getMethod(getter.getName()).invoke(tableProperties);
            assertSame(expected, getter.invoke(properties), getter.getName());
        }
    }

    @Test
    void refusesNoProperties() {
        assertThrows(NullPointerException.class, () -> new TablePropertiesImpl(null));
    }

    @Test
    void toStringListsNothingWhenNoPropertyIsSet() {
        var properties = new TablePropertiesImpl(new org.openl.rules.table.properties.TableProperties());

        assertEquals("{\r\n}\r\n", properties.toString());
    }

    @Test
    void toStringListsTheSetPropertiesInDefinitionOrder() {
        var tableProperties = new org.openl.rules.table.properties.TableProperties();
        tableProperties.setNature("Premium");
        tableProperties.setPriority(3);
        tableProperties.setActive(Boolean.FALSE);
        tableProperties.setOrigin(OriginsEnum.Base);
        tableProperties.setCountry(CountriesEnum.CA, CountriesEnum.US);
        tableProperties.setTags("first", "second");
        tableProperties.setName("rate");

        assertEquals("""
                {\r
                Name = rate\r
                Tags = [first, second]\r
                Country = [Canada, United States]\r
                Origin = Base\r
                Active = false\r
                Priority = 3\r
                Nature = Premium\r
                }\r
                """, new TablePropertiesImpl(tableProperties).toString());
    }
}
