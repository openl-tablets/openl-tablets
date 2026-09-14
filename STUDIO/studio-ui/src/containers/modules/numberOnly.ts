import type React from 'react'

/** What can stand in a whole number, and what can stand in any number at all. */
const WHOLE = /[\d-]/
const ANY = /[\d.eE+-]/

/** The same, for cleaning text that arrives all at once. */
const NOT_WHOLE = /[^\d-]/g
const NOT_ANY = /[^\d.eE+-]/g

/**
 * Keeps a field that holds a number holding nothing else.
 *
 * <p>A key that cannot stand in a number never reaches the field, so the reader never sees a letter appear and
 * then vanish. What is pasted is cleaned the same way before it lands.
 *
 * @param intOnly whether the field holds whole numbers only
 * @return the handlers to give the field
 */
export const numberOnly = (intOnly: boolean | undefined): {
    onKeyDown: (event: React.KeyboardEvent<HTMLInputElement>) => void
    onPaste: (event: React.ClipboardEvent<HTMLInputElement>) => void
} => ({
    onKeyDown: event => {
        // A shortcut and a key that moves or deletes are none of this; only a key that writes a character is.
        const writes = event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey
        if (writes && !(intOnly ? WHOLE : ANY).test(event.key)) {
            event.preventDefault()
        }
    },
    onPaste: event => {
        const pasted = event.clipboardData.getData('text')
        if (pasted !== pasted.replace(intOnly ? NOT_WHOLE : NOT_ANY, '')) {
            event.preventDefault()
        }
    },
})
