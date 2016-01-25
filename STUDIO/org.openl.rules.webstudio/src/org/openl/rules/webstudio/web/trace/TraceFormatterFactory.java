package org.openl.rules.webstudio.web.trace;

/**
 * @author Andrei Astrouski
 */
public class TraceFormatterFactory {

    public static final String FORMAT_TEXT = "txt";
    public static final String FORMAT_XML = "xml";
    public static final String FORMAT_EXCEL = "xls";

    public TraceFormatter getTraceFormatter(String format) {
        TraceFormatter traceFormatter = null;

        if (FORMAT_XML.equalsIgnoreCase(format)) {
            traceFormatter = new XmlTraceFormatter();
        } else if (FORMAT_EXCEL.equalsIgnoreCase(format)) {
            traceFormatter = new ExcelTraceFormatter();
        } else {
            traceFormatter = new RawStringTraceFormatter();
        }
        // to be continued...

        return traceFormatter;
    }

}
