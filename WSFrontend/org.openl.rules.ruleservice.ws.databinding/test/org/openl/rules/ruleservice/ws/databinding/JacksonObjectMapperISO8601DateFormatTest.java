package org.openl.rules.ruleservice.ws.databinding;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.databinding.JacksonObjectMapperFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(Parameterized.class)
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

    @AfterClass
    public static void afterClass() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Parameters(name = "{0} corresponds to {1} in local time zone")
    public static Object[] data() {
        return new Object[][] { { "2016-12-31T22:00:00", createDate(2016, 12, 31, 22) },
                { "2016-12-31T22:00:00Z", createDate(2017, 1, 1, 0) },
                { "2016-12-31T22:00:00+0200", createDate(2016, 12, 31, 22) },
                { "2016-12-31T22:00:00+03:00", createDate(2016, 12, 31, 21) },
                { "2016-12-31T22:00:00-07", createDate(2017, 1, 1, 7) },
                { "2016-12-31", createDate(2016, 12, 31, 0) },
                { "1483142400000", createDate(2016, 12, 31, 2) } };
    }

    private final String json;
    private final Date target;
    private final ObjectMapper objectMapper;

    public JacksonObjectMapperISO8601DateFormatTest(String source, Date target) {
        this.json = "{\n\t\"currentDate\": \"" + source + "\"\n}";
        this.target = target;

        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        bean.setSupportVariations(true);
        this.objectMapper = bean.createJacksonObjectMapper();
    }

    @Test
    public void test() throws Exception {
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
