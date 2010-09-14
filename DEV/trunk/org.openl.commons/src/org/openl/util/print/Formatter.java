/**
 * Created Jan 28, 2007
 */
package org.openl.util.print;

/**
 * @author snshor
 *
 */
public class Formatter {

    private static IFormat defaultFormat = new DefaultFormat();

    public static IFormat defaultFormat() {
        return defaultFormat;
    }

    public static String filterCategory() {
        return OutputFilter.class.getName();
    }

    public static StringBuffer format(Object obj, int mode, StringBuffer buf) {
        Object key = obj == null ? (Object) "null" : (Object) obj.getClass();

        return format(obj, mode, buf, key);
    }

    public static StringBuffer format(Object obj, int mode, StringBuffer buf, Object key) {

        IFormat format = (IFormat) CategorizedSearchContext.current().find(key, formatCategory());

        if (format == null) {
            format = defaultFormat();
        }

        OutputFilter filter = (OutputFilter) CategorizedSearchContext.current().find(key, filterCategory());

        if (filter == null) {
            filter = (OutputFilter) CategorizedSearchContext.current().find(filterCategory(), filterCategory());
        }

        PrintContext cxt = new PrintContext(format, filter);

        return cxt.print(obj, mode, buf);
    }

    public static String formatCategory() {
        return IFormat.class.getName();
    }

    public static void registerFilter(Class<?> c, OutputFilter filter) {
        CategorizedSearchContext.current()
                .register(c == null ? (Object) filterCategory() : c, filterCategory(), filter);
    }

    public static void registerFormat(Class<?> c, IFormat format) {
        CategorizedSearchContext.current().register(c, formatCategory(), format);
    }

    public static void unregisterFormat(Class<?> c) {
        CategorizedSearchContext.current().unregister(c, formatCategory());
    }

}
