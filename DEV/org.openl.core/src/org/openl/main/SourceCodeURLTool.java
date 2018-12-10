/*
 * Created on Dec 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.main;

import java.io.PrintWriter;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor
 */
public class SourceCodeURLTool implements SourceCodeURLConstants {

    static public String makeSourceLocationURL(ILocation location, IOpenSourceCodeModule module) {
        final Logger log = LoggerFactory.getLogger(SourceCodeURLTool.class);

        if (module != null && StringUtils.isEmpty(module.getUri())) {
            return StringUtils.EMPTY;
        }

        int start = -1, end = -1;

        String lineInfo = null;

        if (module == null) {
            return "NO_MODULE";
        }

        if (location != null && location.isTextLocation()) {
            String src = module.getCode();
            TextInfo info = new TextInfo(src);
            try {
                start = location.getStart().getAbsolutePosition(info) + module.getStartPosition();
                end = location.getEnd().getAbsolutePosition(info) + module.getStartPosition();
            } catch (UnsupportedOperationException e) {
                log.warn("Cannot make source location URL", e);
            }

            lineInfo = START + "=" + start + QSEP + END + "=" + end;

        }

        String moduleUri = getUri(module, location);

        String suffix = !moduleUri.contains(QSTART) ? QSTART : QSEP;

        String url = moduleUri;
        if (lineInfo != null) {
            url += suffix + lineInfo;
        } else if (location != null) {
            url += suffix + location;
        }

        return url;
    }

    private static String getUri(IOpenSourceCodeModule module, ILocation location) {
        String moduleUri;
        if (module instanceof CompositeSourceCodeModule && location != null && location.isTextLocation()) {
            int line = location.getStart().getLine(new TextInfo(module.getCode()));

            IOpenSourceCodeModule[] modules = ((CompositeSourceCodeModule) module).getModules();
            if (modules.length <= line || line < 0) {
                // Occurs when Method table expression has several lines but reside inside single cell.
                final Logger log = LoggerFactory.getLogger(SourceCodeURLTool.class);
                log.debug("Modules count in composite module are less than error line number. Return first found module uri.");
                moduleUri = module.getUri();
            } else {
                // Occurs when Method table expression has several lines and each line resides inside his own cell.
                IOpenSourceCodeModule actualModule = modules[line];
                moduleUri = actualModule == null ? module.getUri() : actualModule.getUri();
            }
        } else {
            moduleUri = module.getUri();
        }
        return moduleUri;
    }

    static public void printCodeAndError(ILocation location, IOpenSourceCodeModule module, PrintWriter pw) {

        if (location == null) {
            return;
        }

        if (!location.isTextLocation()) {
            // stream.println(" at " + location);
            return;
        }

        String src = module.getCode();
        TextInfo info = new TextInfo(src);
        String[] lines = src.split("[\\r\\n]+");

        // position = location.getStart().getAbsolutePosition(info);

        pw.println("Openl Code Fragment:");
        pw.println("=======================");

        int line1 = location.getStart().getLine(info);
        int column1 = location.getStart().getColumn(info, 1);

        int line2 = location.getEnd().getLine(info);
        int column2 = location.getEnd().getColumn(info, 1);

        int start = Math.max(line1 - 2, 0);

        int end = Math.min(start + 4, lines.length);

        for (int i = start; i < end; ++i) {
            String line = untab(lines[i], 2);
            pw.println(line);
            if (i == line1) {
                StringBuilder buf = new StringBuilder(Math.max(column1, column2) + 5);
                StringTool.append(buf, ' ', column1);
                int col2 = line1 == line2 ? column2 + 1 : line.length();

                StringTool.append(buf, '^', col2 - column1);
                pw.println(buf.toString());
            }
        }
        pw.println("=======================");

    }

    static public void printSourceLocation(ILocation location, IOpenSourceCodeModule module, PrintWriter pw) {

        String url = SourceCodeURLTool.makeSourceLocationURL(location, module);

        if (!StringUtils.isEmpty(url)) {
            pw.println(SourceCodeURLConstants.AT_PREFIX + url);
        }
    }


    static private String untab(String src, int tabSize) {
        StringBuilder buf = new StringBuilder(src.length() + 10);

        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c != '\t') {
                buf.append(c);
            } else {
                buf.append(' ');

                int extra = buf.length() % tabSize;
                if (extra != 0) {
                    StringTool.append(buf, ' ', tabSize - extra);
                }
            }
        }
        return buf.toString();
    }

}
