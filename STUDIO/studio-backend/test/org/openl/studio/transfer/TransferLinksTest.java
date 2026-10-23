package org.openl.studio.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.openl.studio.projects.service.files.ConflictPolicy;

class TransferLinksTest {

    private static final TransferOperation DOWNLOAD = new TransferOperation.Download("P1", "rules/Rules.xlsx", null,
            null, false);
    private static final TransferOperation UPLOAD = new TransferOperation.Upload("P1", "rules/Rules.xlsx", null,
            ConflictPolicy.OVERWRITE, true, null, null);

    private final TestClock clock = new TestClock(Instant.parse("2026-10-23T10:00:00Z"));
    private final TransferLinks links = new TransferLinks(clock, "PT15M", 3);

    @Test
    void aLinkIsFoundByItsIdUntilItExpires() {
        var issued = links.add("jdoe", "jdoe|pat:1", DOWNLOAD);

        assertEquals(43, issued.id().length());
        assertTrue(issued.id().matches("[A-Za-z0-9_-]+"));
        assertNotEquals(issued.id(), issued.key());
        assertEquals(Instant.parse("2026-10-23T10:15:00Z"), issued.link().expiresAt());
        assertSame(issued.link(), links.find(issued.id()));
        assertSame(issued.link(), links.find(issued.id()));

        clock.advance(Duration.ofMinutes(15));
        assertNull(links.find(issued.id()));
        assertEquals(0, links.size());
    }

    @Test
    void aTakenLinkIsFoundNoMore() {
        var issued = links.add("jdoe", "jdoe|pat:1", UPLOAD);

        assertSame(issued.link(), links.takeUpload(issued.id()));
        assertNull(links.takeUpload(issued.id()));
        assertNull(links.find(issued.id()));
    }

    @Test
    void aDownloadLinkIsNotTakenAsAnUpload() {
        var issued = links.add("jdoe", "jdoe|pat:1", DOWNLOAD);

        assertNull(links.takeUpload(issued.id()));
        assertSame(issued.link(), links.find(issued.id()));
    }

    @Test
    void anExpiredLinkIsNotTaken() {
        var issued = links.add("jdoe", "jdoe|pat:1", UPLOAD);
        clock.advance(Duration.ofMinutes(16));

        assertNull(links.takeUpload(issued.id()));
    }

    @Test
    void anUnknownIdNamesNoLink() {
        links.add("jdoe", "jdoe|pat:1", DOWNLOAD);

        assertNull(links.find("unknown"));
        assertNull(links.takeUpload("unknown"));
    }

    @Test
    void aNewLinkBeyondTheLimitOfAClientEndsItsOldestOne() {
        var client = new ClientTransferLinks(links);
        var first = client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);
        var second = client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);
        var third = client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);

        var fourth = client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);

        assertNull(links.find(first.id()));
        assertSame(second.link(), links.find(second.id()));
        assertSame(third.link(), links.find(third.id()));
        assertSame(fourth.link(), links.find(fourth.id()));
    }

    @Test
    void aLinkUsedOrExpiredLeavesRoomForANewOne() {
        var client = new ClientTransferLinks(links);
        var spent = client.issue("jdoe", "jdoe|pat:1", UPLOAD);
        var kept = client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);
        client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);
        links.takeUpload(spent.id());

        client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);

        assertSame(kept.link(), links.find(kept.id()));
        assertEquals(3, links.size());
    }

    @Test
    void aClientHoldsOneLinkAtLeast() {
        var store = new TransferLinks(clock, "PT15M", 0);
        var client = new ClientTransferLinks(store);

        var issued = client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);

        assertSame(issued.link(), store.find(issued.id()));
    }

    @Test
    void theLinksOfAClientEndWithItsState() {
        var client = new ClientTransferLinks(links);
        var other = new ClientTransferLinks(links);
        var ended = client.issue("jdoe", "jdoe|pat:1", DOWNLOAD);
        var kept = other.issue("jsmith", "jsmith|pat:2", DOWNLOAD);

        client.end();

        assertNull(links.find(ended.id()));
        assertSame(kept.link(), links.find(kept.id()));
    }
}
