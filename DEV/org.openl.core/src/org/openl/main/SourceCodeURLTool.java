/*
 * Created on Dec 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.main;

import java.io.PrintWriter;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
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
                // Should not occur
                final Logger log = LoggerFactory.getLogger(SourceCodeURLTool.class);
                log.warn("Modules count in composite module are less than error line number. Return first found module uri.");
                moduleUri = module.getUri();
            } else {
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
            return;
        }

        String src = module.getCode();
        TextInfo info = new TextInfo(src);
        String[] lines = src.split("[\r\n]+");

        pw.println("Openl Code Fragment:");
        pw.println("=======================");

        int line1 = location.getStart().getLine(info);
        int column1 = location.getStart().getColumn(info, 1);

        int line2 = location.getEnd().getLine(info);
        int column2 = location.getEnd().getColumn(info, 1);

        int start = Math.max(line1 - 2, 0);

        int end = Math.min(start + 4, lines.length);

        for (int i = start; i < end; ++i) {
            String line = lines[i].replace('\t', ' ');
            pw.println(line);
            if (i == line1) {
                for (int i2 = 0; i2 < column1; i2++) {
                    pw.print(' ');
                }
                int col2 = line1 == line2 ? column2 + 1 : line.length();

                for (int i3 = 0; i3 < col2 - column1; i3++) {
                    pw.print('^');
                }
                pw.println();
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

}
