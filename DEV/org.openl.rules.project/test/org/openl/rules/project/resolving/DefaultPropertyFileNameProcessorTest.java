package org.openl.rules.project.resolving;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.table.properties.ITableProperties;

public class DefaultPropertyFileNameProcessorTest {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");

    @Test
    public void unknownPropertyTest() {
        try {
            new DefaultPropertiesFileNameProcessor("%unknownProperty%");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Found unsupported property 'unknownProperty' in file name pattern.", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void lobTest() throws Exception {
        ITableProperties props = new DefaultPropertiesFileNameProcessor(
                "%lob%-%nature%-%state%-%effectiveDate:yyyy-MM-dd%-%startRequestDate:yyyy-MM-dd%")
                .process("AL-BL-CL-GL-NY-2018-07-01-2018-05-03");
        assertArrayEquals(props.getLob(), new String[]{"AL"});
        assertArrayEquals(props.getState(), new UsStatesEnum[]{UsStatesEnum.NY});
        assertEquals(props.getNature(), "BL-CL-GL");
        assertEquals(props.getEffectiveDate(), new Date(118, 6, 1, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(118, 4, 3, 0, 0, 0));

        props = new DefaultPropertiesFileNameProcessor(
                "%lob%-%nature%-%state%-%effectiveDate:yyyyMMdd%-%startRequestDate:yyyyMMdd%")
                .process("AL,BL-CL,GL-DE,OH-20180701-20170621");
        assertArrayEquals(props.getLob(), new String[]{"AL", "BL"});
        assertArrayEquals(props.getState(), new UsStatesEnum[]{UsStatesEnum.DE, UsStatesEnum.OH});
        assertEquals(props.getNature(), "CL,GL");
        assertEquals(props.getEffectiveDate(), new Date(118, 6, 1, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(117, 5, 21, 0, 0, 0));

        props = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%")
                .process("AL,BL-CL,GL-CA-20072019-21062020");
        assertArrayEquals(props.getLob(), new String[]{"AL", "BL-CL", "GL"});
        assertArrayEquals(props.getState(), new UsStatesEnum[]{UsStatesEnum.CA});
        assertNull(props.getNature());
        assertEquals(props.getEffectiveDate(), new Date(119, 6, 20, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(120, 5, 21, 0, 0, 0));
    }

    @Test
    public void testPatternProperty_with_lob_array() throws NoMatchFileNameException,
            InvalidFileNamePatternException,
            ParseException {

        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
                ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%")
                .process("rules/Project-PMT,CMT-01012017-01012018.ext");

        assertArrayEquals(new String[]{"CMT", "PMT"}, properties.getLob());

        assertEquals(dateFormat.parse("01012017"), properties.getEffectiveDate());
        assertEquals(dateFormat.parse("01012018"), properties.getStartRequestDate());
    }

    @Test
    public void testPatternProperty_with_currencies_array() throws NoMatchFileNameException,
            InvalidFileNamePatternException,
            ParseException {

        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
                ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%-%currency%")
                .process("Project-PMT,CMT-01012017-01012018-EUR,UAH.xlsx");

        assertArrayEquals(new String[]{"CMT", "PMT"}, properties.getLob());
        assertArrayEquals(new CurrenciesEnum[]{CurrenciesEnum.EUR, CurrenciesEnum.UAH}, properties.getCurrency());

        assertEquals(dateFormat.parse("01012017"), properties.getEffectiveDate());
        assertEquals(dateFormat.parse("01012018"), properties.getStartRequestDate());
    }

    @Test
    public void testPatternProperty_with_unknownEnumValue_array() throws NoMatchFileNameException,
            InvalidFileNamePatternException {
        assertThrows(NoMatchFileNameException.class, () -> {

            new DefaultPropertiesFileNameProcessor(
                    ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%-%currency%")
                    .process("Project-PMT,CMT-01012017-01012018-EUR,DEFAULT,UAH.xlsx");
        });
    }

    @Test
    public void testPatternProperty_date_separator() throws NoMatchFileNameException,
            InvalidFileNamePatternException,
            ParseException {

        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%startRequestDate:yyyy-MM-dd%").process("path/to.rules/AUTO-FL-2016-01-01");

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.FL}, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns0() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        ITableProperties properties = processor.process("AUTO-CW-20160101.xlsx");

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(UsStatesEnum.values(), properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        ITableProperties properties = processor.process("AUTO-Any-20160101");

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(UsStatesEnum.values(), properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns1() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        ITableProperties properties = processor.process("AUTO-FL,ME-20160101.xlsx");

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.FL, UsStatesEnum.ME}, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns2() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        ITableProperties properties = processor.process("path.to/rules/AUTO-PMT-20160101.xlsx");

        assertArrayEquals(new String[]{"PMT"}, properties.getLob());
        assertArrayEquals(null, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns3() throws InvalidFileNamePatternException {
        try {
            PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                    .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
            processor.process("path.to/rules/Tests.xlsx");
            fail("Ooops...");
        } catch (NoMatchFileNameException e) {
            assertEquals(
                    "File 'path.to/rules/Tests.xlsx' does not match file name pattern 'AUTO-%lob%-%startRequestDate%'.",
                    e.getMessage());
        }
    }

    @Test
    public void testPropertyGroupsWrongDatePattern() throws InvalidFileNamePatternException {
        try {
            new DefaultPropertiesFileNameProcessor("%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%")
                    .process("AUTO-FL,ME-20160101.ext");
            fail("Ooops...");
        } catch (NoMatchFileNameException e) {
            assertEquals(
                    "File 'AUTO-FL,ME-20160101.ext' does not match file name pattern '%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%'.\r\n Invalid property: effectiveDate.\r\n Message: Failed to parse a date '20160101'..",
                    e.getMessage());
        }
    }

    @Test
    public void testPropertyGroupsNegative() {
        try {
            new DefaultPropertiesFileNameProcessor("%lob%-%state%-%effectiveDate,lob%");
            fail("Ooops...");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Property 'lob' is declared in pattern '%lob%-%state%-%effectiveDate,lob%' several times.",
                    e.getMessage());
        }

        try {
            new DefaultPropertiesFileNameProcessor("%lob,nature%-%state%-%effectiveDate%");
            fail("Ooops...");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Incompatible properties in the group: [lob, nature].", e.getMessage());
        }

        try {
            new DefaultPropertiesFileNameProcessor("%lob%-%state,lang%-%effectiveDate%");
            fail("Ooops...");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Incompatible properties in the group: [state, lang].", e.getMessage());
        }

        try {
            new DefaultPropertiesFileNameProcessor("%lob%-%state,foo%-%effectiveDate%");
            fail("Ooops...");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Found unsupported property 'foo' in file name pattern.", e.getMessage());
        }
    }

    @Test
    public void testPropertyGroups() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%effectiveDate,startRequestDate%").process("AUTO-FL,ME-20160101.xlsx");
        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.FL, UsStatesEnum.ME}, properties.getState());
        Date date = dateFormat.parse("01012016");
        assertEquals(date, properties.getStartRequestDate());
        assertEquals(date, properties.getEffectiveDate());
    }

    @Test
    public void testPropertyGroups1() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%").process("AUTO-FL,ME-01012016");
        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.FL, UsStatesEnum.ME}, properties.getState());
        Date date = dateFormat.parse("01012016");
        assertEquals(date, properties.getStartRequestDate());
        assertEquals(date, properties.getEffectiveDate());
    }

    @Test
    public void testFolder() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        DefaultPropertiesFileNameProcessor processor1 = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%startRequestDate%");
        assertMatch(processor1, "AUTO-NY-20200712");
        assertMatch(processor1, "AUTO-NY-20200712.xlsx");
        assertMatch(processor1, "rules/AUTO-NY-20200712");
        assertMatch(processor1, "rules/AUTO-NY-20200712.ext");
        assertMatch(processor1, "rules/AUTO/AUTO-NY-20200712");
        assertMatch(processor1, "rules/AUTO/AUTO-NY-20200712.txt");

        DefaultPropertiesFileNameProcessor processor2 = new DefaultPropertiesFileNameProcessor(
                "%lob%/%state%/*%startRequestDate%");
        assertMatch(processor2, "AUTO/NY/UP.20200712");
        assertMatch(processor2, "AUTO/NY/UP-20200712");
        assertMatch(processor2, "AUTO/NY/UP.20200712.ext");
        assertMatch(processor2, "AUTO/NY/UP-20200712.ext");
        assertMatch(processor2, "rules/AUTO/NY/UP.20200712");
        assertMatch(processor2, "rules/AUTO/NY/UP-20200712");
        assertMatch(processor2, "rules/AUTO/NY/UP.20200712.ext");
        assertMatch(processor2, "rules/AUTO/NY/UP-20200712.ext");

        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/*%startRequestDate%"),
                "AUTO/NY/UP.20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/**/*%startRequestDate%"),
                "AUTO/NY/UP.20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/??.%startRequestDate%"),
                "AUTO/NY/UP.20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/**/??.%startRequestDate%"),
                "AUTO/NY/UP.20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/*/%state%/*%startRequestDate%"),
                "AUTO/AL/NY/UP.20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/**/%state%/*%startRequestDate%"),
                "AUTO/AL/AL/NY/UP.20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/**/%state%/*%startRequestDate%"),
                "AUTO/AL/NY/UP.20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/**/%state%/*%startRequestDate%"),
                "AUTO/NY/UP.20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/%lob%/UP/**/%state%/*%startRequestDate%"),
                "AUTO/UP/NY/20200712.xlsx");

        assertMatch(new DefaultPropertiesFileNameProcessor("/*/UP/**/%lob%-%state%/%startRequestDate%"),
                "AUTO/UP/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/*/UP/**/%lob%-%state%/%startRequestDate%"),
                "AUTO/UP/DOWN/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/*/UP/**/%lob%-%state%/%startRequestDate%"),
                "AUTO/UP/DOWN/Any/AUTO-NY/20200712.xlsx");

        assertMatch(new DefaultPropertiesFileNameProcessor("/**/UP/**/%lob%-%state%/%startRequestDate%"),
                "UP/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/**/UP/**/%lob%-%state%/%startRequestDate%"),
                "DOWN/UP/DOWN/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/**/UP/**/%lob%-%state%/%startRequestDate%"),
                "Any/DOWN/UP/Any2/DOWN/AUTO-NY/20200712.xlsx");

        assertMatch(new DefaultPropertiesFileNameProcessor("/?/UP/**/%lob%-%state%/%startRequestDate%"),
                "A/UP/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/?/UP/**/%lob%-%state%/%startRequestDate%"),
                "Я/UP/DOWN/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/?/UP/**/%lob%-%state%/%startRequestDate%"),
                "$/UP/Any/DOWN/AUTO-NY/20200712.xlsx");

        assertMatch(new DefaultPropertiesFileNameProcessor("/./UP$/**/%lob%-%state%/%startRequestDate%"),
                "./UP$/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./UP+/**/%lob%-%state%/%startRequestDate%"),
                "./UP+/DOWN/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./UP-/**/%lob%-%state%/%startRequestDate%"),
                "./UP-/Any/DOWN/AUTO-NY/20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./UP^/**/%lob%-%state%/%startRequestDate%"),
                "./UP^/Any/DOWN/AUTO-NY/20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./UP(/**/%lob%-%state%/%startRequestDate%"),
                "./UP(/Any/DOWN/AUTO-NY/20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./UP)/**/%lob%-%state%/%startRequestDate%"),
                "./UP)/Any/DOWN/AUTO-NY/20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./(UP)/**/%lob%-%state%/%startRequestDate%"),
                "./(UP)/Any/DOWN/AUTO-NY/20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./[UP]+/**/%lob%-%state%/%startRequestDate%"),
                "./[UP]+/Any/DOWN/AUTO-NY/20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./U()P/**/%lob%-%state%/%startRequestDate%"),
                "./U()P/Any/DOWN/AUTO-NY/20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./U(%lob%)P/**/*-%state%/%startRequestDate%"),
                "./U(AUTO)P/Any/DOWN/AUTO-NY/20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./(U/tur/P)/**/%lob%-%state%/%startRequestDate%"),
                "./(U/tur/P)/Any/DOWN/AUTO-NY/20200712");
        assertMatch(new DefaultPropertiesFileNameProcessor("/./(U/**/P)/**/%lob%-%state%/%startRequestDate%"),
                "./(U/t/u/r/P)/Any/DOWN/AUTO-NY/20200712");

    }

    @Test
    public void testFolderNoMatch() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        DefaultPropertiesFileNameProcessor processor1 = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%startRequestDate%");
        assertNotMatch(processor1, "AUTO--20200712");
        assertNotMatch(processor1, "AUTO-NY-20200712/test");
        assertNotMatch(processor1, "AUTO-NY-20200712/test.xlsx");
        assertNotMatch(processor1, "AUTO/-NY-20200712");
        assertNotMatch(processor1, "AUTO-/NY-20200712");

        DefaultPropertiesFileNameProcessor processor2 = new DefaultPropertiesFileNameProcessor(
                "%lob%/%state%/*%startRequestDate%");
        assertNotMatch(processor2, "AUTO/NY-20200712");
        assertNotMatch(processor2, "AUTO/ALNY/20200712");
        assertNotMatch(processor2, "AUTO/NY/UP/20200712.ext");
        assertNotMatch(processor2, "AUTO/NY/UP-/20200712.ext");

        assertNotMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/*%startRequestDate%"),
                "rules/AUTO/NY/UP.20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/*/*%startRequestDate%"),
                "rules/AUTO/NY/UP.20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("test/%lob%/%state%/*%startRequestDate%"),
                "rules/AUTO/NY/UP.20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("les/%lob%/%state%/*%startRequestDate%"),
                "rules/AUTO/NY/UP.20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("rules/%lob%/%state%/*%startRequestDate%"),
                "les/AUTO/NY/UP.20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/**/*%startRequestDate%"),
                "rules/AUTO/NY/UP.20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/**/...%startRequestDate%"),
                "AUTO/NY/UP.20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/%lob%/%state%/?.%startRequestDate%"),
                "AUTO/NY/UP.20200712");

        assertNotMatch(new DefaultPropertiesFileNameProcessor("/*/UP/**/%lob%-%state%/%startRequestDate%"),
                "AUTO/UPS/UP/AUTO-NY/20200712.xlsx");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/*/UP/**/%lob%-%state%/%startRequestDate%"),
                "AUTO/UPS/UP/DOWN/AUTO-NY/20200712.xlsx");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/*/UP/**/%lob%-%state%/%startRequestDate%"),
                "AUTO/UPS/UP/DOWN/Any/AUTO-NY/20200712.xlsx");

        assertNotMatch(new DefaultPropertiesFileNameProcessor("/**/UP/**/%lob%-%state%/%startRequestDate%"),
                "UPS/AUTO-NY/20200712.xlsx");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/**/UP/**/%lob%-%state%/%startRequestDate%"),
                "DOWN/UPS/DOWN/AUTO-NY/20200712.xlsx");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/**/UP/**/%lob%-%state%/%startRequestDate%"),
                "Any/DOWN/UPS/Any2/DOWN/AUTO-NY/20200712.xlsx");

        assertNotMatch(new DefaultPropertiesFileNameProcessor("/?/UP/**/%lob%-%state%/%startRequestDate%"),
                "UP/AUTO-NY/20200712.xlsx");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/?/UP/**/%lob%-%state%/%startRequestDate%"),
                "UP/DOWN/AUTO-NY/20200712.xlsx");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/?/UP/**/%lob%-%state%/%startRequestDate%"),
                "UP/Any/DOWN/AUTO-NY/20200712.xlsx");

        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./UP/**/%lob%-%state%/%startRequestDate%"),
                "A/UP/AUTO-NY/20200712.xlsx");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./UP/**/%lob%-%state%/%startRequestDate%"),
                "Я/UP/DOWN/AUTO-NY/20200712.xlsx");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./UP/**/%lob%-%state%/%startRequestDate%"),
                "$/UP/Any/DOWN/AUTO-NY/20200712.xlsx");

        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./UP(/**/%lob%-%state%/%startRequestDate%"),
                "./UP((/Any/DOWN/AUTO-NY/20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./UP)/**/%lob%-%state%/%startRequestDate%"),
                "./UP))/Any/DOWN/AUTO-NY/20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./(UP)/**/%lob%-%state%/%startRequestDate%"),
                "./((UP))/Any/DOWN/AUTO-NY/20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./[UP]+/**/%lob%-%state%/%startRequestDate%"),
                "./UP/Any/DOWN/AUTO-NY/20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./U()P/**/%lob%-%state%/%startRequestDate%"),
                "./UP/Any/DOWN/AUTO-NY/20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./U(%lob%)P/**/*-%state%/%startRequestDate%"),
                "./UAUTOP/Any/DOWN/AUTO-NY/20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./(U/tur/P)/**/%lob%-%state%/%startRequestDate%"),
                "./U/tur/P/Any/DOWN/AUTO-NY/20200712");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("/./(U/**/P)/**/%lob%-%state%/%startRequestDate%"),
                "./U/t/u/r/P/Any/DOWN/AUTO-NY/20200712");

    }

    @Test
    public void testRegexp() throws InvalidFileNamePatternException, NoMatchFileNameException, ParseException {

        assertMatch(new DefaultPropertiesFileNameProcessor(".*-%lob%-%state%-%startRequestDate%"),
                "D1234-AUTO-NY-20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("D.*-%lob%-%state%-%startRequestDate%"),
                "D1234-AUTO-NY-20200712.xlsx");
        assertMatch(new DefaultPropertiesFileNameProcessor("D\\d\\d\\d\\d-%lob%-%state%-%startRequestDate%"),
                "D1234-AUTO-NY-20200712.xls");
        assertMatch(new DefaultPropertiesFileNameProcessor("D\\d{4}-%lob%-%state%-%startRequestDate%"),
                "D1234-AUTO-NY-20200712.xlsx");

        assertNotMatch(new DefaultPropertiesFileNameProcessor("D.*-%lob%-%state%-%startRequestDate%"),
                "E12345-AUTO-NY-20200712.xls");
        assertNotMatch(new DefaultPropertiesFileNameProcessor(".*-%lob%-%state%-%startRequestDate%"),
                "AUTO-NY-20200712.xls");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("D\\d\\d\\d\\d-%lob%-%state%-%startRequestDate%"),
                "D124-AUTO-NY-20200712.xls");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("D\\d\\d\\d\\d-%lob%-%state%-%startRequestDate%"),
                "D12345-AUTO-NY-20200712.xls");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("D\\d{4}-%lob%-%state%-%startRequestDate%"),
                "D123-AUTO-NY-20200712.xls");
        assertNotMatch(new DefaultPropertiesFileNameProcessor("D\\d{4}-%lob%-%state%-%startRequestDate%"),
                "D12345-AUTO-NY-20200712.xls");
    }

    private void assertNotMatch(DefaultPropertiesFileNameProcessor processor, String file) {
        var exc = assertThrows(NoMatchFileNameException.class, () -> processor.process(file));
        assertTrue(exc.getMessage().startsWith("File '" + file + "' does not match file name pattern"));
    }

    private void assertMatch(DefaultPropertiesFileNameProcessor processor,
                             String fileName) throws NoMatchFileNameException, ParseException {
        ITableProperties properties = processor.process(fileName);

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.NY}, properties.getState());
        assertEquals(dateFormat.parse("12072020"), properties.getStartRequestDate());
    }

}
