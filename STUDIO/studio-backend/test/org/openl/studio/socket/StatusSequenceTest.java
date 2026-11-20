package org.openl.studio.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

class StatusSequenceTest {

    @Test
    void aNumberTakenLaterIsLargerEvenWhenTheClockStandsStill() {
        var sequence = new StatusSequence(() -> 1_000);

        assertEquals(1_000, sequence.next());
        assertEquals(1_001, sequence.next());
        assertEquals(1_002, sequence.next());
    }

    @Test
    void theNumbersCatchUpWithTheClock() {
        var clock = new AtomicLong(1_000);
        var sequence = new StatusSequence(clock::get);
        sequence.next();
        sequence.next();

        clock.set(5_000);

        assertEquals(5_000, sequence.next());
    }

    @Test
    void aClockGoingBackNeverMakesANumberSmaller() {
        var clock = new AtomicLong(5_000);
        var sequence = new StatusSequence(clock::get);
        sequence.next();

        clock.set(1_000);

        assertEquals(5_001, sequence.next());
    }

    @Test
    void theNumbersOfAServerStartedAgainAreLargerThanTheOnesBefore() {
        var before = new StatusSequence().next();

        var after = new StatusSequence().next();

        assertTrue(after >= before, "a restarted server must not number its statuses as older than the ones before");
    }

    @Test
    void theNumbersFollowTheMicrosecondsOfTheWallClockAndFitAJavaScriptNumber() {
        var from = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());

        var number = new StatusSequence().next();

        assertTrue(number >= from && number <= ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()));
        assertTrue(number < 1L << 53, "a browser reads the number as a double");
    }

    @Test
    void numbersTakenAtOnceAreAllDistinct() {
        var sequence = new StatusSequence(() -> 1_000);

        var distinct = LongStream.range(0, 10_000).parallel().map(ignored -> sequence.next()).distinct().count();

        assertEquals(10_000, distinct);
    }
}
