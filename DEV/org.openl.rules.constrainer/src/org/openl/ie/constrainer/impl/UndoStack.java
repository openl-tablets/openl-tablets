package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.tools.FastStack;

/**
 * An implementation of the undo stack.
 */
public final class UndoStack implements java.io.Serializable {
    private final FastStack _stack;

    /**
     * Default constructor.
     */
    public UndoStack() {
        _stack = new FastStack();
    }

    /**
     * Restores the state of this stack to the newSize.
     */
    public void backtrack(int newSize) {
        int size = _stack.size();

        if (newSize > size) {
            Constrainer.abort("Internal error in UndoStack.backtrack(): newSize > size");
        }

        int nUndos = size - newSize;
        while (nUndos-- > 0) {
            Undo undo_object = popUndo();
            undo_object.undo();
        }
    }

    /**
     * Returns true if this stack is empty.
     */
    public boolean empty() {
        return _stack.empty();
    }

    /**
     * Pops the undo from this stack and returns it.
     */
    Undo popUndo() {
        return (Undo) _stack.pop();
    }

    /**
     * Pushes the undo onto this stack.
     */
    public void pushUndo(Undo undo) {
        _stack.push(undo);
    }

    /**
     * Returns the size of this stack.
     */
    public int size() {
        return _stack.size();
    }

    @Override
    public String toString() {
        return "UndoStack: " + _stack;
    }

} // ~UndoStack
