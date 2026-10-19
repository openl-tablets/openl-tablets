package org.openl.studio.audit.rest.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.studio.audit.model.AuditEventView;
import org.openl.studio.audit.service.AuditService;

/**
 * Writes events of the audit log as CSV (RFC 4180): a header line, then a line for every event.
 *
 * <p>The file starts with a byte order mark, so a spreadsheet application reads it as UTF-8. The time is written in
 * ISO 8601 in UTC, the kinds of things by their codes in the REST API, the details as a JSON object.
 *
 * <p>A value that a spreadsheet application would read as a formula is written as text, with a leading apostrophe:
 * a name chosen by an attacker, such as the user name of a failed sign-in, could otherwise run in the spreadsheet of
 * the administrator who opens the file.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class AuditCsvWriter {

    private static final String BYTE_ORDER_MARK = "\uFEFF";
    private static final String LINE_END = "\r\n";
    private static final String HEADER = String.join(",", "id", "time", "actor", "authentication", "clientAddress",
            "category", "action", "outcome", "targetType", "target", "subjectType", "subject", "details");

    private final Writer writer;

    void writeHeader() throws IOException {
        writer.write(BYTE_ORDER_MARK);
        writer.write(HEADER);
        writer.write(LINE_END);
    }

    /**
     * Writes the line of an event.
     *
     * @throws UncheckedIOException when the event cannot be written
     */
    void write(AuditEventView event) {
        var line = Arrays.stream(new Object[]{
                event.id(),
                event.time(),
                event.actor(),
                AuditService.code(event.authentication()),
                event.clientAddress(),
                AuditService.code(event.category()),
                AuditService.code(event.action()),
                AuditService.code(event.outcome()),
                AuditService.code(event.targetType()),
                event.target(),
                AuditService.code(event.subjectType()),
                event.subject(),
                event.details().isEmpty() ? null : AuditService.toJson(event.details())
        }).map(AuditCsvWriter::cell).collect(Collectors.joining(","));
        try {
            writer.write(line);
            writer.write(LINE_END);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void flush() throws IOException {
        writer.flush();
    }

    private static String cell(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        var text = value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
