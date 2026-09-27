package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.OriginsEnum;

class TablePropertiesTest {

    @Test
    void toStringListsNothingWhenNoPropertyIsSet() {
        var properties = new TableProperties(new org.openl.rules.table.properties.TableProperties());

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
                """, new TableProperties(tableProperties).toString());
    }
}
