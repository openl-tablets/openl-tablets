package org.openl.studio.audit.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditAuthentication;
import org.openl.studio.audit.model.AuditCategory;
import org.openl.studio.audit.model.AuditEventView;
import org.openl.studio.audit.model.AuditOutcome;
import org.openl.studio.audit.model.AuditSubjectType;
import org.openl.studio.audit.model.AuditTargetType;

class AuditCsvWriterTest {

    @Test
    void writesAHeaderAndALinePerEvent() throws IOException {
        var out = new StringWriter();
        var csv = new AuditCsvWriter(out);

        csv.writeHeader();
        csv.write(AuditEventView.builder()
                .id(2)
                .time(Instant.parse("2026-10-19T10:15:30Z"))
                .actor("jdoe")
                .authentication(AuditAuthentication.FORM)
                .clientAddress("10.0.0.7")
                .category(AuditCategory.AUTHENTICATION)
                .action(AuditAction.SIGN_IN)
                .outcome(AuditOutcome.FAILURE)
                .targetType(AuditTargetType.PROJECT)
                .target("design:/rules/Sales, Europe")
                .subjectType(AuditSubjectType.GROUP)
                .subject("Analysts")
                .details(Map.of("reason", "badCredentials"))
                .build());
        csv.write(AuditEventView.builder()
                .id(1)
                .time(Instant.parse("2026-10-19T10:15:00Z"))
                .category(AuditCategory.AUDIT)
                .action(AuditAction.AUDIT_LOG_PURGED)
                .outcome(AuditOutcome.SUCCESS)
                .details(Map.of())
                .build());
        csv.flush();

        assertEquals("""
                \uFEFFid,time,actor,authentication,clientAddress,category,action,outcome,targetType,target,\
                subjectType,subject,details\r
                2,2026-10-19T10:15:30Z,jdoe,form,10.0.0.7,authentication,signIn,failure,project,\
                "design:/rules/Sales, Europe",group,Analysts,"{""reason"":""badCredentials""}"\r
                1,2026-10-19T10:15:00Z,,,,audit,auditLogPurged,success,,,,,\r
                """, out.toString());
    }

    @Test
    void writesAValueASpreadsheetWouldRunAsText() {
        var out = new StringWriter();

        new AuditCsvWriter(out).write(AuditEventView.builder()
                .id(1)
                .time(Instant.EPOCH)
                .actor("=HYPERLINK(\"http://evil\")")
                .category(AuditCategory.AUTHENTICATION)
                .action(AuditAction.SIGN_IN)
                .outcome(AuditOutcome.FAILURE)
                .targetType(AuditTargetType.USER)
                .target("@SUM(A1)")
                .subjectType(AuditSubjectType.USER)
                .subject("+1\nline")
                .details(Map.of())
                .build());

        assertEquals("""
                1,1970-01-01T00:00:00Z,"'=HYPERLINK(""http://evil"")",,,authentication,signIn,failure,\
                user,'@SUM(A1),user,"'+1
                line",\r
                """, out.toString());
    }

    @Test
    void reportsALineThatCannotBeWritten() {
        var broken = new Writer() {
            @Override
            public void write(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("The client went away");
            }

            @Override
            public void flush() {
                // nothing is buffered
            }

            @Override
            public void close() {
                // nothing to release
            }
        };
        var event = AuditEventView.builder()
                .id(1)
                .time(Instant.EPOCH)
                .category(AuditCategory.AUDIT)
                .action(AuditAction.AUDIT_LOG_READ)
                .outcome(AuditOutcome.SUCCESS)
                .details(Map.of())
                .build();
        var csv = new AuditCsvWriter(broken);

        assertThrows(UncheckedIOException.class, () -> csv.write(event));
    }
}
