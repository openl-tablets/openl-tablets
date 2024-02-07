package org.openl.rules.serialization.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBean;

public class JacksonObjectMapperISO8601DateFormatTest {

    private static final Locale DEFAULT_LOCALE;
    private static final TimeZone DEFAULT_TIMEZONE;

    static {
        DEFAULT_TIMEZONE = TimeZone.getDefault();
        DEFAULT_LOCALE = Locale.getDefault();

        Locale.setDefault(Locale.US);

        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);
    }

    @AfterAll
    public static void afterClass() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    public static Object[] data() {
        return new Object[][]{{"2016-12-31T22:00:00", createDate(2016, 12, 31, 22)},
                {"2016-12-31T22:00:00Z", createDate(2017, 1, 1, 0)},
                {"2016-12-31T22:00:00+0200", createDate(2016, 12, 31, 22)},
                {"2016-12-31T22:00:00+03:00", createDate(2016, 12, 31, 21)},
                {"2016-12-31T22:00:00-07", createDate(2017, 1, 1, 7)},
                {"2016-12-31", createDate(2016, 12, 31, 0)},
                {"1483142400000", createDate(2016, 12, 31, 2)}};
    }

    private String json;
    private Date target;
    private ObjectMapper objectMapper;

    public void initJacksonObjectMapperISO8601DateFormatTest(String source, Date target) {
        this.json = "{\n\t\"currentDate\": \"" + source + "\"\n}";
        this.target = target;

        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        bean.setSupportVariations(true);
        ObjectMapper objectMapper = null;
        try {
            objectMapper = bean.createJacksonObjectMapper();
        } catch (ClassNotFoundException ignored) {
        }
        this.objectMapper = objectMapper;
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0} corresponds to {1} in local time zone")
    public void test(String source, Date target) throws Exception {
        initJacksonObjectMapperISO8601DateFormatTest(source, target);
        IRulesRuntimeContext context = objectMapper.readValue(json, IRulesRuntimeContext.class);
        assertEquals(target, context.getCurrentDate());
    }

    private static Date createDate(int year, int month, int dayOfMonth, int hour) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, hour, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
