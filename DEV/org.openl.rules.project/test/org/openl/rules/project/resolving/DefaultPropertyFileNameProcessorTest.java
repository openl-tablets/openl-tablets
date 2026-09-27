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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.UsStatesEnum;

class DefaultPropertyFileNameProcessorTest {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");

    @Test
    void unknownPropertyTest() {
        try {
            new DefaultPropertiesFileNameProcessor("%unknownProperty%");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Found unsupported property 'unknownProperty' in file name pattern.", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    void lobTest() throws Exception {
        var props = new DefaultPropertiesFileNameProcessor(
                "%lob%-%nature%-%state%-%effectiveDate:yyyy-MM-dd%-%startRequestDate:yyyy-MM-dd%")
                .process("AL-BL-CL-GL-NY-2018-07-01-2018-05-03");
        assertArrayEquals(new String[]{"AL"}, props.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.NY}, props.getState());
        assertEquals("BL-CL-GL", props.getNature());
        assertEquals(new Date(118, 6, 1, 0, 0, 0), props.getEffectiveDate());
        assertEquals(new Date(118, 4, 3, 0, 0, 0), props.getStartRequestDate());

        props = new DefaultPropertiesFileNameProcessor(
                "%lob%-%nature%-%state%-%effectiveDate:yyyyMMdd%-%startRequestDate:yyyyMMdd%")
                .process("AL,BL-CL,GL-DE,OH-20180701-20170621");
        assertArrayEquals(new String[]{"AL", "BL"}, props.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.DE, UsStatesEnum.OH}, props.getState());
        assertEquals("CL,GL", props.getNature());
        assertEquals(new Date(118, 6, 1, 0, 0, 0), props.getEffectiveDate());
        assertEquals(new Date(117, 5, 21, 0, 0, 0), props.getStartRequestDate());

        props = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%")
                .process("AL,BL-CL,GL-CA-20072019-21062020");
        assertArrayEquals(new String[]{"AL", "BL-CL", "GL"}, props.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.CA}, props.getState());
        assertNull(props.getNature());
        assertEquals(new Date(119, 6, 20, 0, 0, 0), props.getEffectiveDate());
        assertEquals(new Date(120, 5, 21, 0, 0, 0), props.getStartRequestDate());
    }

    @Test
    void testPatternProperty_with_lob_array() throws NoMatchFileNameException,
            InvalidFileNamePatternException,
            ParseException {

        var properties = new DefaultPropertiesFileNameProcessor(
                ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%")
                .process("rules/Project-PMT,CMT-01012017-01012018.ext");

        assertArrayEquals(new String[]{"CMT", "PMT"}, properties.getLob());

        assertEquals(dateFormat.parse("01012017"), properties.getEffectiveDate());
        assertEquals(dateFormat.parse("01012018"), properties.getStartRequestDate());
    }

    @Test
    void testPatternProperty_with_currencies_array() throws NoMatchFileNameException,
            InvalidFileNamePatternException,
            ParseException {

        var properties = new DefaultPropertiesFileNameProcessor(
                ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%-%currency%")
                .process("Project-PMT,CMT-01012017-01012018-EUR,UAH.xlsx");

        assertArrayEquals(new String[]{"CMT", "PMT"}, properties.getLob());
        assertArrayEquals(new CurrenciesEnum[]{CurrenciesEnum.EUR, CurrenciesEnum.UAH}, properties.getCurrency());

        assertEquals(dateFormat.parse("01012017"), properties.getEffectiveDate());
        assertEquals(dateFormat.parse("01012018"), properties.getStartRequestDate());
    }

    @Test
    void testPatternProperty_with_unknownEnumValue_array() {
        assertThrows(NoMatchFileNameException.class, () -> {

            new DefaultPropertiesFileNameProcessor(
                    ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%-%currency%")
                    .process("Project-PMT,CMT-01012017-01012018-EUR,DEFAULT,UAH.xlsx");
        });
    }

    @Test
    void testPatternProperty_date_separator() throws NoMatchFileNameException,
            InvalidFileNamePatternException,
            ParseException {

        var properties = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%startRequestDate:yyyy-MM-dd%").process("path/to.rules/AUTO-FL-2016-01-01");

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.FL}, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    void testMultiPatterns0() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        var properties = processor.process("AUTO-CW-20160101.xlsx");

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(UsStatesEnum.values(), properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    void testMultiPatterns() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        var properties = processor.process("AUTO-Any-20160101");

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(UsStatesEnum.values(), properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    void testMultiPatterns1() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        var properties = processor.process("AUTO-FL,ME-20160101.xlsx");

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.FL, UsStatesEnum.ME}, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    void testMultiPatterns2() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        var properties = processor.process("path.to/rules/AUTO-PMT-20160101.xlsx");

        assertArrayEquals(new String[]{"PMT"}, properties.getLob());
        assertArrayEquals(null, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    void testMultiPatterns3() throws InvalidFileNamePatternException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        var e = assertThrows(NoMatchFileNameException.class, () -> processor.process("path.to/rules/Tests.xlsx"));
        assertEquals(
                "File 'path.to/rules/Tests.xlsx' does not match file name pattern 'AUTO-%lob%-%startRequestDate%'.",
                e.getMessage());
    }

    @Test
    void testPropertyGroupsWrongDatePattern() throws InvalidFileNamePatternException {
        var processor = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%");
        var e = assertThrows(NoMatchFileNameException.class, () -> processor.process("AUTO-FL,ME-20160101.ext"));
        assertEquals(
                "File 'AUTO-FL,ME-20160101.ext' does not match file name pattern '%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%'.\r\n Invalid property: effectiveDate.\r\n Message: Failed to parse a date '20160101'..",
                e.getMessage());
    }

    @Test
    void testPropertyGroupsNegative() {
        var e = assertThrows(InvalidFileNamePatternException.class,
                () -> new DefaultPropertiesFileNameProcessor("%lob%-%state%-%effectiveDate,lob%"));
        assertEquals("Property 'lob' is declared in pattern '%lob%-%state%-%effectiveDate,lob%' several times.",
                e.getMessage());

        e = assertThrows(InvalidFileNamePatternException.class,
                () -> new DefaultPropertiesFileNameProcessor("%lob,nature%-%state%-%effectiveDate%"));
        assertEquals("Incompatible properties in the group: [lob, nature].", e.getMessage());

        e = assertThrows(InvalidFileNamePatternException.class,
                () -> new DefaultPropertiesFileNameProcessor("%lob%-%state,lang%-%effectiveDate%"));
        assertEquals("Incompatible properties in the group: [state, lang].", e.getMessage());

        e = assertThrows(InvalidFileNamePatternException.class,
                () -> new DefaultPropertiesFileNameProcessor("%lob%-%state,foo%-%effectiveDate%"));
        assertEquals("Found unsupported property 'foo' in file name pattern.", e.getMessage());
    }

    @Test
    void testPropertyGroups() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        var properties = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%effectiveDate,startRequestDate%").process("AUTO-FL,ME-20160101.xlsx");
        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.FL, UsStatesEnum.ME}, properties.getState());
        var date = dateFormat.parse("01012016");
        assertEquals(date, properties.getStartRequestDate());
        assertEquals(date, properties.getEffectiveDate());
    }

    @Test
    void testPropertyGroups1() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        var properties = new DefaultPropertiesFileNameProcessor(
                "%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%").process("AUTO-FL,ME-01012016");
        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.FL, UsStatesEnum.ME}, properties.getState());
        var date = dateFormat.parse("01012016");
        assertEquals(date, properties.getStartRequestDate());
        assertEquals(date, properties.getEffectiveDate());
    }

    @ParameterizedTest(name = "{0} matches {1}")
    @CsvSource(delimiter = '|', textBlock = """
            %lob%-%state%-%startRequestDate%                 | AUTO-NY-20200712
            %lob%-%state%-%startRequestDate%                 | AUTO-NY-20200712.xlsx
            %lob%-%state%-%startRequestDate%                 | rules/AUTO-NY-20200712
            %lob%-%state%-%startRequestDate%                 | rules/AUTO-NY-20200712.ext
            %lob%-%state%-%startRequestDate%                 | rules/AUTO/AUTO-NY-20200712
            %lob%-%state%-%startRequestDate%                 | rules/AUTO/AUTO-NY-20200712.txt
            %lob%/%state%/*%startRequestDate%                | AUTO/NY/UP.20200712
            %lob%/%state%/*%startRequestDate%                | AUTO/NY/UP-20200712
            %lob%/%state%/*%startRequestDate%                | AUTO/NY/UP.20200712.ext
            %lob%/%state%/*%startRequestDate%                | AUTO/NY/UP-20200712.ext
            %lob%/%state%/*%startRequestDate%                | rules/AUTO/NY/UP.20200712
            %lob%/%state%/*%startRequestDate%                | rules/AUTO/NY/UP-20200712
            %lob%/%state%/*%startRequestDate%                | rules/AUTO/NY/UP.20200712.ext
            %lob%/%state%/*%startRequestDate%                | rules/AUTO/NY/UP-20200712.ext
            /%lob%/%state%/*%startRequestDate%               | AUTO/NY/UP.20200712
            /%lob%/%state%/**/*%startRequestDate%            | AUTO/NY/UP.20200712
            /%lob%/%state%/??.%startRequestDate%             | AUTO/NY/UP.20200712
            /%lob%/%state%/**/??.%startRequestDate%          | AUTO/NY/UP.20200712
            /%lob%/*/%state%/*%startRequestDate%             | AUTO/AL/NY/UP.20200712
            /%lob%/**/%state%/*%startRequestDate%            | AUTO/AL/AL/NY/UP.20200712
            /%lob%/**/%state%/*%startRequestDate%            | AUTO/AL/NY/UP.20200712
            /%lob%/**/%state%/*%startRequestDate%            | AUTO/NY/UP.20200712
            /%lob%/UP/**/%state%/*%startRequestDate%         | AUTO/UP/NY/20200712.xlsx
            /*/UP/**/%lob%-%state%/%startRequestDate%        | AUTO/UP/AUTO-NY/20200712.xlsx
            /*/UP/**/%lob%-%state%/%startRequestDate%        | AUTO/UP/DOWN/AUTO-NY/20200712.xlsx
            /*/UP/**/%lob%-%state%/%startRequestDate%        | AUTO/UP/DOWN/Any/AUTO-NY/20200712.xlsx
            /**/UP/**/%lob%-%state%/%startRequestDate%       | UP/AUTO-NY/20200712.xlsx
            /**/UP/**/%lob%-%state%/%startRequestDate%       | DOWN/UP/DOWN/AUTO-NY/20200712.xlsx
            /**/UP/**/%lob%-%state%/%startRequestDate%       | Any/DOWN/UP/Any2/DOWN/AUTO-NY/20200712.xlsx
            /?/UP/**/%lob%-%state%/%startRequestDate%        | A/UP/AUTO-NY/20200712.xlsx
            /?/UP/**/%lob%-%state%/%startRequestDate%        | Я/UP/DOWN/AUTO-NY/20200712.xlsx
            /?/UP/**/%lob%-%state%/%startRequestDate%        | $/UP/Any/DOWN/AUTO-NY/20200712.xlsx
            /./UP$/**/%lob%-%state%/%startRequestDate%       | ./UP$/AUTO-NY/20200712.xlsx
            /./UP+/**/%lob%-%state%/%startRequestDate%       | ./UP+/DOWN/AUTO-NY/20200712.xlsx
            /./UP-/**/%lob%-%state%/%startRequestDate%       | ./UP-/Any/DOWN/AUTO-NY/20200712.xlsx
            /./UP^/**/%lob%-%state%/%startRequestDate%       | ./UP^/Any/DOWN/AUTO-NY/20200712
            /./UP(/**/%lob%-%state%/%startRequestDate%       | ./UP(/Any/DOWN/AUTO-NY/20200712
            /./UP)/**/%lob%-%state%/%startRequestDate%       | ./UP)/Any/DOWN/AUTO-NY/20200712
            /./(UP)/**/%lob%-%state%/%startRequestDate%      | ./(UP)/Any/DOWN/AUTO-NY/20200712
            /./[UP]+/**/%lob%-%state%/%startRequestDate%     | ./[UP]+/Any/DOWN/AUTO-NY/20200712
            /./U()P/**/%lob%-%state%/%startRequestDate%      | ./U()P/Any/DOWN/AUTO-NY/20200712
            /./U(%lob%)P/**/*-%state%/%startRequestDate%     | ./U(AUTO)P/Any/DOWN/AUTO-NY/20200712
            /./(U/tur/P)/**/%lob%-%state%/%startRequestDate% | ./(U/tur/P)/Any/DOWN/AUTO-NY/20200712
            /./(U/**/P)/**/%lob%-%state%/%startRequestDate%  | ./(U/t/u/r/P)/Any/DOWN/AUTO-NY/20200712
            """)
    void testFolder(String pattern, String fileName)
            throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        assertMatch(new DefaultPropertiesFileNameProcessor(pattern), fileName);
    }

    @ParameterizedTest(name = "{0} does not match {1}")
    @CsvSource(delimiter = '|', textBlock = """
            %lob%-%state%-%startRequestDate%                 | AUTO--20200712
            %lob%-%state%-%startRequestDate%                 | AUTO-NY-20200712/test
            %lob%-%state%-%startRequestDate%                 | AUTO-NY-20200712/test.xlsx
            %lob%-%state%-%startRequestDate%                 | AUTO/-NY-20200712
            %lob%-%state%-%startRequestDate%                 | AUTO-/NY-20200712
            %lob%/%state%/*%startRequestDate%                | AUTO/NY-20200712
            %lob%/%state%/*%startRequestDate%                | AUTO/ALNY/20200712
            %lob%/%state%/*%startRequestDate%                | AUTO/NY/UP/20200712.ext
            %lob%/%state%/*%startRequestDate%                | AUTO/NY/UP-/20200712.ext
            /%lob%/%state%/*%startRequestDate%               | rules/AUTO/NY/UP.20200712
            /%lob%/%state%/*/*%startRequestDate%             | rules/AUTO/NY/UP.20200712
            test/%lob%/%state%/*%startRequestDate%           | rules/AUTO/NY/UP.20200712
            les/%lob%/%state%/*%startRequestDate%            | rules/AUTO/NY/UP.20200712
            rules/%lob%/%state%/*%startRequestDate%          | les/AUTO/NY/UP.20200712
            /%lob%/%state%/**/*%startRequestDate%            | rules/AUTO/NY/UP.20200712
            /%lob%/%state%/**/...%startRequestDate%          | AUTO/NY/UP.20200712
            /%lob%/%state%/?.%startRequestDate%              | AUTO/NY/UP.20200712
            /*/UP/**/%lob%-%state%/%startRequestDate%        | AUTO/UPS/UP/AUTO-NY/20200712.xlsx
            /*/UP/**/%lob%-%state%/%startRequestDate%        | AUTO/UPS/UP/DOWN/AUTO-NY/20200712.xlsx
            /*/UP/**/%lob%-%state%/%startRequestDate%        | AUTO/UPS/UP/DOWN/Any/AUTO-NY/20200712.xlsx
            /**/UP/**/%lob%-%state%/%startRequestDate%       | UPS/AUTO-NY/20200712.xlsx
            /**/UP/**/%lob%-%state%/%startRequestDate%       | DOWN/UPS/DOWN/AUTO-NY/20200712.xlsx
            /**/UP/**/%lob%-%state%/%startRequestDate%       | Any/DOWN/UPS/Any2/DOWN/AUTO-NY/20200712.xlsx
            /?/UP/**/%lob%-%state%/%startRequestDate%        | UP/AUTO-NY/20200712.xlsx
            /?/UP/**/%lob%-%state%/%startRequestDate%        | UP/DOWN/AUTO-NY/20200712.xlsx
            /?/UP/**/%lob%-%state%/%startRequestDate%        | UP/Any/DOWN/AUTO-NY/20200712.xlsx
            /./UP/**/%lob%-%state%/%startRequestDate%        | A/UP/AUTO-NY/20200712.xlsx
            /./UP/**/%lob%-%state%/%startRequestDate%        | Я/UP/DOWN/AUTO-NY/20200712.xlsx
            /./UP/**/%lob%-%state%/%startRequestDate%        | $/UP/Any/DOWN/AUTO-NY/20200712.xlsx
            /./UP(/**/%lob%-%state%/%startRequestDate%       | ./UP((/Any/DOWN/AUTO-NY/20200712
            /./UP)/**/%lob%-%state%/%startRequestDate%       | ./UP))/Any/DOWN/AUTO-NY/20200712
            /./(UP)/**/%lob%-%state%/%startRequestDate%      | ./((UP))/Any/DOWN/AUTO-NY/20200712
            /./[UP]+/**/%lob%-%state%/%startRequestDate%     | ./UP/Any/DOWN/AUTO-NY/20200712
            /./U()P/**/%lob%-%state%/%startRequestDate%      | ./UP/Any/DOWN/AUTO-NY/20200712
            /./U(%lob%)P/**/*-%state%/%startRequestDate%     | ./UAUTOP/Any/DOWN/AUTO-NY/20200712
            /./(U/tur/P)/**/%lob%-%state%/%startRequestDate% | ./U/tur/P/Any/DOWN/AUTO-NY/20200712
            /./(U/**/P)/**/%lob%-%state%/%startRequestDate%  | ./U/t/u/r/P/Any/DOWN/AUTO-NY/20200712
            """)
    void testFolderNoMatch(String pattern, String fileName) throws InvalidFileNamePatternException {
        assertNotMatch(new DefaultPropertiesFileNameProcessor(pattern), fileName);
    }

    @Test
    void testRegexp() throws InvalidFileNamePatternException, NoMatchFileNameException, ParseException {

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
        var properties = processor.process(fileName);

        assertArrayEquals(new String[]{"AUTO"}, properties.getLob());
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.NY}, properties.getState());
        assertEquals(dateFormat.parse("12072020"), properties.getStartRequestDate());
    }

}
