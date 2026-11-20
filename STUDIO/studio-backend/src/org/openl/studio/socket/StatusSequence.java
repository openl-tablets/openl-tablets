package org.openl.studio.socket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.springframework.stereotype.Component;

/**
 * Numbers the moments the server reads a state it tells the clients about, over REST and over the socket.
 *
 * <p>A larger number was taken later, whichever thread took it. A client that hears of the same state both ways —
 * a status it read and a status pushed to it — keeps the one with the larger number, and does not depend on the
 * order the two arrive in. A subscription started at a number misses nothing told after it.
 *
 * <p>The numbers follow the microseconds of the wall clock, so the ones taken after a restart of the server are
 * larger than the ones taken before it. They stay below 2<sup>53</sup>, so a JavaScript number holds them exactly.
 */
@Component
public class StatusSequence {

    private final AtomicLong last = new AtomicLong();
    private final LongSupplier clock;

    public StatusSequence() {
        this(() -> ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()));
    }

    StatusSequence(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * The number of this moment: larger than every number taken before it, and never behind the clock.
     */
    public long next() {
        var now = clock.getAsLong();
        return last.accumulateAndGet(now, (previous, time) -> Math.max(previous + 1, time));
    }
}
