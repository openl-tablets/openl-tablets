package org.openl.rules.serialization.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.serialization.JacksonObjectMapperFactoryBean;

public class JacksonObjectMapperISO8601DateFormatTest {

    private static final Locale DEFAULT_LOCALE;
    private static final TimeZone DEFAULT_TIMEZONE;
    private static ObjectMapper mapper;

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

    @BeforeAll
    public static void init() throws Exception {
        mapper = new JacksonObjectMapperFactoryBean().createJacksonObjectMapper();
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

    @MethodSource("data")
    @ParameterizedTest(name = "Date deserialization to {1} in local time zone from \"{0}\"")
    public void test(String source, Date target) throws Exception {
        assertEquals(target, mapper.readValue("\"" + source + "\"", Date.class));
    }

    public static String[] zdt() {
        return new String[] {
                "2016-12-31T22:00:00Z",
                "2016-12-31T22:00:00+03:00",
                "2016-12-31T22:00:00-07:00",
        };
    }

    @MethodSource("zdt")
    @ParameterizedTest(name = "ZonedDateTime serialization/deserialization of {0} ")
    public void ZonedDateTimeTest(String date) throws Exception {
        var str = "\"" + date + "\"";
        var zdt = ZonedDateTime.parse(date);
        assertEquals(zdt, mapper.readValue(str, ZonedDateTime.class));
        assertEquals(str, mapper.writeValueAsString(zdt));
    }

    private static Date createDate(int year, int month, int dayOfMonth, int hour) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, hour, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
