/**
 * Created Feb 26, 2007
 */
package org.openl.rules.table.xls.formatters;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.openl.util.Log;
import org.openl.util.NumberUtils;
import org.openl.util.StringTool;
import org.openl.util.formatters.ConstTextFormatter;
import org.openl.util.formatters.IFormatter;
import org.openl.util.formatters.NumberTextFormatter;

/**
 * This class provides default conversion of MS Excel formats to Java formats.
 * There is no way for practical and technical reasons to map it completely
 * 100%. Therefore this class will be supplemented by a) pre-defined hardcoded
 * mapping for most of embedded MS Excel formats; b) by providing app developers
 * with ability to plug-in their own custom transformers and/or mappings.
 *
 * @author snshor
 *
 *  TODO: refactor this class. Remove from method parse() next parameters: SegmentFormatter positiveFormat,
 *  SegmentFormatter negativeFormat, SegmentFormatter zeroFormat;
 *
 */
public class XlsNumberFormatter extends AXlsFormatter {

    public static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.00");

    public static final String GENERAL_FORMAT_STR = "#.######";

    public static final DecimalFormat GENERAL_FORMAT = new DecimalFormat(GENERAL_FORMAT_STR);

    public static final XlsNumberFormatter GENERAL = new XlsNumberFormatter(new SegmentFormatter(
            new NumberTextFormatter(GENERAL_FORMAT), null), null, null);

    private static final String[] COLORS_STR = { "[Black]", "[Blue]", "[Cyan]", "[Green]", "[Magenta]", "[Red]",
        "[White]", "[Yellow]" };

    private static final short[][] COLORS = { { 0x00, 0x00, 0x00 }, { 0x00, 0x00, 0xff }, { 0x00, 0xff, 0xff },
            { 0x00, 0xff, 0x00 }, { 0xff, 0x00, 0xff }, { 0xff, 0x00, 0x00 }, { 0xff, 0xff, 0xff },
            { 0xff, 0xff, 0x00 }, };

    private SegmentFormatter positiveFormat;

    private SegmentFormatter negativeFormat;

    private SegmentFormatter zeroFormat;
   
    private static String detectColor(SegmentFormatter segmentFormatter, String format) {
        for (int i = 0; i < COLORS_STR.length; i++) {
            if (format.contains(COLORS_STR[i]) || format.contains(COLORS_STR[i].toUpperCase())) {
                segmentFormatter.setColor(COLORS[i]);
                return format.substring(COLORS_STR[i].length());
            }
        }

        return format;
    }

    private static SegmentFormatter getFormat(String format, Map<String, SegmentFormatter> existingFmts, 
            boolean isNegative) {
        SegmentFormatter segmentFormatter = existingFmts.get(format);
        if (segmentFormatter != null) {
            return segmentFormatter;
        }

        segmentFormatter = makeSegmentFormatter(format, isNegative);
        existingFmts.put(format, segmentFormatter);
        return segmentFormatter;
    }

    public static void main(String[] args) {

        //System.out.println(new DecimalFormat("$#,##0").format(12.334) + "|");

        //System.out.println(new DecimalFormat(" #,##0.0; (#,##0.0)").format(-12.334) + "|");

        double[] x = { -12.35, 12345.6789, 8.9, .631, 12, 12.35, };

        String[] formats = { "$#,##0_);[Red]($#,##0)", "_(* #,##0.0_);_(* (#,##0.0);_(* \"-\"??_);_(@_)", "#,###",
                "#,", "####.#", "#.000", "0.#", "#.0#", "???.???", "#.0;(#.0)", };

        HashMap<String, SegmentFormatter> existingFmts = new HashMap<String, SegmentFormatter>();
        for (int i = 0; i < formats.length; i++) {
            XlsNumberFormatter xnf = makeFormat(formats[i], existingFmts);
            // NumberFormat f = new DecimalFormat(jf);
            System.out.println(formats[i] + "  :  ");
            for (int j = 0; j < x.length; j++) {
                Double value = new Double(x[j]);

                System.out.println(xnf.format(value) + "|  " + x[j]);
            }
            System.out.println();
        }        
    }

    public static XlsNumberFormatter makeFormat(String xlsformat, Map<String, SegmentFormatter> existingFmts) {
        String[] fmts = StringTool.tokenize(xlsformat, ";");

        int N = 3;
        int NEG = 1;
        SegmentFormatter[] sff = new SegmentFormatter[N];
        int len = Math.min(fmts.length, N);

        for (int i = 0; i < len; ++i) {
            SegmentFormatter sf = getFormat(fmts[i], existingFmts, i == NEG);
            sff[i] = sf;
        }

        return new XlsNumberFormatter(sff[0], sff[1], sff[2]);

    }

    private static SegmentFormatter makeSegmentFormatter(String format, boolean isNegative) {

        SegmentFormatter segmentFormatter = new SegmentFormatter();

        format = detectColor(segmentFormatter, format);

        String javaFormat = transformToJavaFormat(format, segmentFormatter);
        if (isNegative) {
            javaFormat = StringTool.removeChars(javaFormat, "()") + ';' + javaFormat;
        }

        if (javaFormat.indexOf('#') < 0 && javaFormat.indexOf('0') < 0) {
            IFormatter textFormatter = new ConstTextFormatter();
            segmentFormatter.setFormatter(textFormatter);
            return segmentFormatter;

        }

        DecimalFormat decimalFormat = null;
        try {
            decimalFormat = new DecimalFormat(javaFormat);
        } catch (Throwable t) {
            Log.warn("Bad java format. Using default. Consider custom mapping: '" + javaFormat + "'");
            decimalFormat = DEFAULT_FORMAT;
        }

        IFormatter textFormatter = new NumberTextFormatter(decimalFormat);
        segmentFormatter.setFormatter(textFormatter);

        return segmentFormatter;
    }

    public static String transformToJavaFormat(String xlsFormat, SegmentFormatter segmentFormatter) {

        StringBuffer sb = new StringBuffer();

        boolean skip = false;

        for (int i = 0; i < xlsFormat.length(); i++) {
            if (skip) {
                skip = false;
                continue;
            }
            char c = xlsFormat.charAt(i);

            switch (c) {
                case '_':
                    sb.append(' ');
                    skip = true;
                    continue;
                case '*':
                    skip = true;
                    continue;
                case '\\':
                case '"':
                    continue;
                default:
                    sb.append(c);
            }
        }

        xlsFormat = sb.toString();

        // transform trailing commas

        while (xlsFormat.endsWith(",")) {
            double newMultiplier = segmentFormatter.getMultiplier() / 1000;
            segmentFormatter.setMultiplier(newMultiplier);
            xlsFormat = xlsFormat.substring(0, xlsFormat.length() - 1);
        }

        // TODO sure it works differently, but do we want to deal with it? Not
        // for
        // now.
        if (xlsFormat.indexOf(".?") >= 0) {
            xlsFormat = xlsFormat.replace('?', '#');
        } else {
            xlsFormat = xlsFormat.replace('?', ' ');
        }

        return xlsFormat;
    }

    public XlsNumberFormatter(SegmentFormatter positiveFormat, SegmentFormatter negativeFormat, SegmentFormatter zeroFormat) {
        this.positiveFormat = positiveFormat;
        this.negativeFormat = negativeFormat;
        this.zeroFormat = zeroFormat;
    }

    public String format(Object value, SegmentFormatter segmentFormatter) {
        if (!(value instanceof Number)) {
            Log.error("Should be Number " + value);
            return null;
        }
        Number number = (Number) value;
        if (segmentFormatter == null) {
            segmentFormatter = getSegmentFormatter(number);
        }
        if (segmentFormatter.getMultiplier() != 1) {
            value = new Double(number.doubleValue() * segmentFormatter.getMultiplier());
        }
        return segmentFormatter.getFormatter().format(value);
    }
    
    public String format(Object value) {
        return format(value, null);
    }

    public SegmentFormatter getSegmentFormatter(Number number) {
        SegmentFormatter segmentFormatter = positiveFormat;
        if (number.doubleValue() < 0) {
            segmentFormatter = isNegativeFormat();
        } else if (number.doubleValue() == 0) {
            segmentFormatter = zeroFormat();
        }
        return segmentFormatter;
    }

    private SegmentFormatter isNegativeFormat() {
        return negativeFormat == null ? positiveFormat : negativeFormat;
    }
    
    public Object parse(String value) {

        try {
            double doubleValue = Double.parseDouble(value);
            return NumberUtils.intOrDouble(doubleValue);
        } catch (NumberFormatException ex) {

        }

        Object firstTry = positiveFormat.parse(value);
        if (firstTry != value) {
            // parse
            return firstTry;
        }

        if (negativeFormat != null) {
            firstTry = negativeFormat.parse(value);
            if (firstTry != value) {
                // from
                // parse
                return firstTry;
            }
        } else {
            return value;
        }

        if (zeroFormat != null) {
            firstTry = zeroFormat.parse(value);
            if (firstTry != value) {
                // from
                // parse
                return firstTry;
            }
        } else {
            return value;
        }

        return value;
    }

    private SegmentFormatter zeroFormat() {
        return zeroFormat == null ? positiveFormat : zeroFormat;
    }

}
