package org.openl.rules.project.resolving;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.DefaultPropertiesFileNameProcessor.PatternModel;
import org.openl.rules.table.properties.ITableProperties;

public class DefaultPropertyFileNameProcessorTest {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
    private DefaultPropertiesFileNameProcessor processor;

    @Before
    public void setUp() {
        processor = new DefaultPropertiesFileNameProcessor();
    }

    @Test
    public void unknownPropertyTest() {
        try {
            new PatternModel("%unknownProperty%");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Found unsupported property 'unknownProperty' in file name pattern.", e.getMessage());
            return;
        }
        Assert.fail();
    }

    @Test
    public void lobTest() throws Exception {
        DefaultPropertiesFileNameProcessor processor = new DefaultPropertiesFileNameProcessor();

        ITableProperties props = processor.process("AL-BL-CL-GL-NY-2018-07-01-2018-05-03",
            "%lob%-%nature%-%state%-%effectiveDate:yyyy-MM-dd%-%startRequestDate:yyyy-MM-dd%");
        assertArrayEquals(props.getLob(), new String[] { "AL" });
        assertArrayEquals(props.getState(), new UsStatesEnum[] { UsStatesEnum.NY });
        assertEquals(props.getNature(), "BL-CL-GL");
        assertEquals(props.getEffectiveDate(), new Date(118, 06, 01, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(118, 04, 03, 0, 0, 0));

        props = processor.process("AL,BL-CL,GL-DE,OH-20180701-20170621",
            "%lob%-%nature%-%state%-%effectiveDate:yyyyMMdd%-%startRequestDate:yyyyMMdd%");
        assertArrayEquals(props.getLob(), new String[] { "AL", "BL" });
        assertArrayEquals(props.getState(), new UsStatesEnum[] { UsStatesEnum.DE, UsStatesEnum.OH });
        assertEquals(props.getNature(), "CL,GL");
        assertEquals(props.getEffectiveDate(), new Date(118, 06, 01, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(117, 05, 21, 0, 0, 0));

        props = processor.process("AL,BL-CL,GL-CA-20072019-21062020",
            "%lob%-%state%-%effectiveDate:ddMMyy%-%startRequestDate:ddMMyyyy%");
        assertArrayEquals(props.getLob(), new String[] { "AL", "BL-CL", "GL" });
        assertArrayEquals(props.getState(), new UsStatesEnum[] { UsStatesEnum.CA });
        assertNull(props.getNature());
        assertEquals(props.getEffectiveDate(), new Date(119, 06, 20, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(120, 05, 21, 0, 0, 0));
    }

    @Test
    public void testPatternProperty_with_lob_array() throws NoMatchFileNameException,
                                                     InvalidFileNamePatternException,
                                                     ParseException {

        ITableProperties properties = processor.process(mockModule("Project-PMT,CMT-01012017-01012018"),
            ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%");

        assertArrayEquals(new String[] { "CMT", "PMT" }, properties.getLob());

        assertEquals(dateFormat.parse("01012017"), properties.getEffectiveDate());
        assertEquals(dateFormat.parse("01012018"), properties.getStartRequestDate());
    }

    @Test
    public void testPatternProperty_with_currencies_array() throws NoMatchFileNameException,
                                                            InvalidFileNamePatternException,
                                                            ParseException {

        ITableProperties properties = processor.process(mockModule("Project-PMT,CMT-01012017-01012018-EUR,UAH"),
            ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%-%currency%");

        assertArrayEquals(new String[] { "CMT", "PMT" }, properties.getLob());
        assertArrayEquals(new CurrenciesEnum[] { CurrenciesEnum.EUR, CurrenciesEnum.UAH }, properties.getCurrency());

        assertEquals(dateFormat.parse("01012017"), properties.getEffectiveDate());
        assertEquals(dateFormat.parse("01012018"), properties.getStartRequestDate());
    }

    @Test(expected = NoMatchFileNameException.class)
    public void testPatternProperty_with_unknownEnumValue_array() throws NoMatchFileNameException,
                                                                  InvalidFileNamePatternException {

        processor.process(mockModule("Project-PMT,CMT-01012017-01012018-EUR,DEFAULT,UAH"),
            ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%-%currency%");
    }

    @Test
    public void testPatternProperty_date_separator() throws NoMatchFileNameException,
                                                     InvalidFileNamePatternException,
                                                     ParseException {

        ITableProperties properties = processor.process(mockModule("AUTO-FL-2016-01-01"),
            "%lob%-%state%-%startRequestDate:yyyy-MM-dd%");

        assertArrayEquals(new String[] { "AUTO" }, properties.getLob());
        assertArrayEquals(new UsStatesEnum[] { UsStatesEnum.FL }, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    private static <T> void assertArrayEquals(T[] expectedArray, T[] actualArray) {
        assertNotNull(actualArray);
        assertEquals(expectedArray.length, actualArray.length);
        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(expectedArray[i], actualArray[i]);
        }
    }

    private static Module mockModule(String moduleName) {
        Module module = mock(Module.class);
        when(module.getName()).thenReturn(moduleName);
        return module;
    }

}
