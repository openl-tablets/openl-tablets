package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.table.properties.TableProperties;

/**
 * Verifies the form a property value takes on the copy API: a date in ISO-8601, read back as the same date.
 */
class TablePropertyTextTest {

    @Test
    void writesADateInIsoRatherThanInTheFormOfALocale() {
        assertEquals("2009-01-01", TablePropertyText.format("effectiveDate", date("2009-01-01T00:00")));
    }

    @Test
    void keepsTheTimeOfDayAValueCarries() {
        // A date property may name a moment of the day, which a day alone would silently drop.
        assertEquals("2026-08-21T14:33", TablePropertyText.format("effectiveDate", date("2026-08-21T14:33")));
        assertEquals("2026-08-21T14:33:12", TablePropertyText.format("createdOn", date("2026-08-21T14:33:12")));
        // Down to the millisecond: a moment rounded off would read back as another one.
        assertEquals("2026-08-21T14:33:12.345", TablePropertyText.format("createdOn", date("2026-08-21T14:33:12.345")));
    }

    @Test
    void writesTheDayAPropertyClosingAPeriodNames() {
        // The engine keeps an expiration date at the close of its day. That moment is the engine's, so the day the
        // table declares is what crosses — the way the Table Details editor shows it.
        assertEquals("2009-12-31",
                TablePropertyText.format("expirationDate", date("2009-12-31T23:59:59.999")));
        assertEquals("2009-12-31", TablePropertyText.format("endRequestDate", date("2009-12-31T23:59:59.999")));
        // Read back, it stands for the same moment again: the engine stamps the end of the day onto it.
        assertEquals(date("2009-12-31T23:59:59.999"),
                TableProperties.preprocess("expirationDate", TablePropertyText.parse("expirationDate", "2009-12-31")));
    }

    @Test
    void readsBackTheMomentItWroteWhole() {
        var moment = date("2026-08-21T14:33:12.345");

        assertEquals(moment, TablePropertyText.parse("createdOn", TablePropertyText.format("createdOn", moment)));
    }

    @Test
    void writesEveryOtherValueTheWayTheTableDetailsEditorShowsIt() {
        assertEquals("AL,CA",
                TablePropertyText.format("state", new UsStatesEnum[]{UsStatesEnum.AL, UsStatesEnum.CA}));
        assertEquals("true", TablePropertyText.format("active", Boolean.TRUE));
        assertEquals("copied table", TablePropertyText.format("description", "copied table"));
        // A property the definitions do not know is written as it stands.
        assertEquals("as it stands", TablePropertyText.format("unknown", "as it stands"));
        assertNull(TablePropertyText.format("description", null));
    }

    @Test
    void readsBackTheDateItWrote() {
        assertEquals(date("2009-01-01T00:00"), TablePropertyText.parse("effectiveDate", "2009-01-01"));
        assertEquals(date("2026-08-21T14:33:12"), TablePropertyText.parse("createdOn", "2026-08-21T14:33:12"));
        // The form the dialog was answered with before dates were carried in ISO is still read as that date.
        assertEquals(date("2009-01-01T00:00"), TablePropertyText.parse("effectiveDate", "01/01/2009 12:00 AM"));
    }

    @Test
    void readsAValueAsThePropertyItNames() {
        assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.AL, UsStatesEnum.CA},
                (UsStatesEnum[]) TablePropertyText.parse("state", "AL, CA"));
        // 'No' is one of the ways the engine reads false.
        assertEquals(Boolean.FALSE, TablePropertyText.parse("active", "No"));
        assertEquals("0.0.2", TablePropertyText.parse("version", "0.0.2"));
    }

    @Test
    void keepsATextThePropertyCannotBeReadFrom() {
        // Written to the copy as it stands, so what an author typed is reported by the module rather than dropped.
        assertEquals("whenever", TablePropertyText.parse("effectiveDate", "whenever"));
        assertEquals("Atlantis", TablePropertyText.parse("state", "Atlantis"));
        assertEquals("maybe", TablePropertyText.parse("active", "maybe"));
        assertEquals("anything", TablePropertyText.parse("unknown", "anything"));
    }

    private static Date date(String isoLocal) {
        return Date.from(LocalDateTime.parse(isoLocal).atZone(ZoneId.systemDefault()).toInstant());
    }
}
