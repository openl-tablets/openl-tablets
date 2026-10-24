/*
 * Created on Dec 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.main;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceRange;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * @author snshor
 */
@Slf4j
public final class SourceCodeURLTool {

    private SourceCodeURLTool() {
    }

    private static final Pattern NEW_LINE = Pattern.compile("[\r\n]+");

    public static String makeSourceLocationURL(ILocation location, IOpenSourceCodeModule module) {
        if (module == null) {
            return "NO_MODULE";
        }
        var uri = module.getUri();
        if (StringUtils.isEmpty(uri)) {
            return StringUtils.EMPTY;
        }
        if (location == null || !location.isTextLocation()) {
            return location == null ? uri : withQuery(uri, location.toString());
        }
        var range = rangeOf(location, module);
        // The end of the URL points at the last character of the piece.
        return range == null ? withQuery(uri, "start=-1&end=-1")
                : withQuery(range.uri(), "start=" + range.start() + "&end=" + (range.end() - 1));
    }

    private static String withQuery(String uri, String query) {
        return uri + (uri.contains("?") ? "&" : "?") + query;
    }

    /**
     * Returns the piece of the code the location points at, in the source the code was read from. For code written
     * in a cell, it is the cell: the part after {@code =} is shifted to its place in the cell's text, and a Method
     * table body, which joins the texts of its cells, gives the cell that holds the piece.
     * <p>
     * A location that points at no text covers the whole code. A piece that runs over several cells of a Method
     * table body ends with the cell it starts in.
     *
     * @return the piece, or {@code null} when the location cannot be read in the code or the source has no URI
     */
    public static @Nullable SourceRange rangeOf(@Nullable ILocation location, IOpenSourceCodeModule module) {
        var code = module.getCode();
        if (code == null) {
            return null;
        }
        var start = 0;
        var end = code.length();
        if (location != null && location.isTextLocation()) {
            var info = new TextInfo(code);
            try {
                start = location.getStart().getAbsolutePosition(info);
                // The end position of a syntax node points at its last character.
                end = Math.min(location.getEnd().getAbsolutePosition(info) + 1, code.length());
            } catch (UnsupportedOperationException e) {
                log.debug("The location '{}' points at no position in the code", location, e);
                return null;
            }
        }
        var source = module;
        for (var part = partAt(source, start); part != null; part = partAt(source, start)) {
            source = part.module();
            start += part.shift();
            end = Math.min(end + part.shift(), source.getCode().length());
        }
        var uri = source.getUri();
        return uri == null || start < 0 || end < start ? null : new SourceRange(uri, source.getCode(), start, end);
    }

    /**
     * Returns the source the code of the module was cut from, with the shift from a position in the code to its
     * position in that source, or {@code null} when the module is the source itself.
     */
    private static @Nullable Part partAt(IOpenSourceCodeModule module, int position) {
        if (module instanceof SubTextSourceCodeModule subText) {
            return new Part(subText.getBaseModule(), subText.getStartPosition());
        }
        return module instanceof CompositeSourceCodeModule composite ? partOf(composite, position) : null;
    }

    /**
     * Returns the module of the composite that holds the position. A position after the code falls into the last
     * module.
     */
    private static @Nullable Part partOf(CompositeSourceCodeModule composite, int position) {
        var modules = Arrays.stream(composite.getModules()).filter(Objects::nonNull).toList();
        var length = modules.stream().mapToInt(module -> module.getCode().length()).sum();
        // The code of a composite joins the codes of its modules, each followed by the same separator.
        var separator = modules.isEmpty() ? 0 : (composite.getCode().length() - length) / modules.size();
        var offset = 0;
        for (var module : modules) {
            var next = offset + module.getCode().length() + separator;
            if (position < next || module == modules.getLast()) {
                return new Part(module, -offset);
            }
            offset = next;
        }
        return null;
    }

    /**
     * The source a code was cut from, and the shift from a position in the code to its position in the source.
     */
    private record Part(IOpenSourceCodeModule module, int shift) {
    }

    /**
     * Prints source code fragment and underlines a fragment where an error had been occurred
     *
     * <p>
     * Openl Code Fragment:<br>
     * =======================<br>
     * Double getRAWPremium(Policy p)<br>
     * ^^^^^^<br>
     * =======================<br>
     * </p>
     *
     * @param location source location
     * @param src      source code
     * @param pw       writer
     */
    public static void printCodeAndError(ILocation location, String src, PrintWriter pw) {

        if (location == null) {
            return;
        }

        if (!location.isTextLocation()) {
            return;
        }

        var info = new TextInfo(src);
        var lines = NEW_LINE.split(src);

        pw.print("Openl Code Fragment:\r\n");
        pw.print("=======================\r\n");

        var line1 = location.getStart().getLine(info);
        var column1 = location.getStart().getColumn(info);

        var line2 = location.getEnd().getLine(info);
        var column2 = location.getEnd().getColumn(info);

        var start = Math.max(line1 - 2, 0);

        var end = Math.min(start + 4, lines.length);

        for (var i = start; i < end; ++i) {
            var line = lines[i].replace('\t', ' ');
            pw.print(line);
            pw.print("\r\n");
            if (i == line1) {
                for (var i2 = 0; i2 < column1 - 1; i2++) {
                    pw.print(' ');
                }
                int col2 = line1 == line2 ? (column2 + 1) : line.length();

                for (var i3 = 0; i3 < col2 - column1; i3++) {
                    pw.print('^');
                }
                pw.print("\r\n");
            }
        }
        pw.print("=======================\r\n");

    }

    public static void printSourceLocation(String sourceLocation, PrintWriter pw) {
        if (!StringUtils.isEmpty(sourceLocation)) {
            pw.print("    at ");
            pw.print(sourceLocation);
            pw.print("\r\n");
        }
    }

}
