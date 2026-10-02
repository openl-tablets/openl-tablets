package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;
import static org.openl.ie.constrainer.IntEvent.REMOVE;
import static org.openl.ie.constrainer.IntEvent.VALUE;

import java.util.BitSet;

import org.jspecify.annotations.Nullable;

/**
 * A constrained integer variable.
 * <p>
 * A domain of less than {@value #LARGE_DOMAIN_SIZE} values keeps every value, so any value can be removed from it. A
 * large domain keeps only its bounds, so it loses a value only from its ends.
 * <p>
 * The first change after a choice point saves the state of the domain to restore on backtracking. The observers
 * learn about the changes when the constrainer propagates them.
 */
class IntVar extends IntExp {

    private static final int LARGE_DOMAIN_SIZE = 128;

    private final int offset;
    private @Nullable BitSet values;
    private int min;
    private int max;
    // The changes since the last propagation.
    private int events;
    private int oldMin;
    private int oldMax;
    // The state is saved for the current choice point.
    private boolean undoSaved;

    IntVar(Constrainer constrainer, int min, int max, String name) {
        super(constrainer, name);
        this.offset = min;
        this.min = min;
        this.max = max;
        this.oldMin = min;
        this.oldMax = max;
        var size = (long) max - min + 1;
        if (size < LARGE_DOMAIN_SIZE) {
            var bits = new BitSet((int) size);
            bits.set(0, (int) size);
            values = bits;
        }
    }

    @Override
    int min() {
        return min;
    }

    @Override
    int max() {
        return max;
    }

    @Override
    boolean contains(int value) {
        return min <= value && value <= max && (values == null || values.get(value - offset));
    }

    @Override
    void setMin(int value) throws Failure {
        if (value <= min) {
            return;
        }
        if (value > max) {
            throw new Failure();
        }
        saveUndo();
        // The maximum is in the domain, so there is a value from the given one to the maximum.
        min = values == null ? value : values.nextSetBit(value - offset) + offset;
        changed(MIN);
    }

    @Override
    void setMax(int value) throws Failure {
        if (value >= max) {
            return;
        }
        if (value < min) {
            throw new Failure();
        }
        saveUndo();
        // The minimum is in the domain, so there is a value from the minimum to the given one.
        max = values == null ? value : values.previousSetBit(value - offset) + offset;
        changed(MAX);
    }

    @Override
    void removeValue(int value) throws Failure {
        if (value == min) {
            setMin(value + 1);
        } else if (value == max) {
            setMax(value - 1);
        } else if (values != null && contains(value)) {
            saveUndo();
            values.clear(value - offset);
            changed(REMOVE);
        }
    }

    /**
     * Notifies the observers about the changes since the last propagation.
     */
    void propagate() throws Failure {
        var event = new IntEvent(events, min, max, oldMin, oldMax);
        events = 0;
        oldMin = min;
        oldMax = max;
        notifyObservers(event);
    }

    void allowUndo() {
        undoSaved = false;
    }

    private void changed(int event) {
        events |= event;
        if (min == max) {
            events = (events & ~REMOVE) | VALUE;
        }
        constrainer.enqueue(this);
    }

    private void saveUndo() {
        if (undoSaved) {
            return;
        }
        undoSaved = true;
        constrainer.undoSaved(this);
        var savedMin = min;
        var savedMax = max;
        var savedValues = values == null ? null : (BitSet) values.clone();
        constrainer.addUndo(() -> {
            min = savedMin;
            max = savedMax;
            values = savedValues;
            events = 0;
            oldMin = savedMin;
            oldMax = savedMax;
        });
    }
}
