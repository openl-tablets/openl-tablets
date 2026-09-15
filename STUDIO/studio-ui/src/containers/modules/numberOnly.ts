import type React from 'react'

/** What can stand in a whole number, and what can stand in any number at all. */
const WHOLE = /[\d-]/
const ANY = /[\d.eE+-]/

/**
 * Keeps a field that holds a number holding nothing else.
 *
 * <p>A key that cannot stand in a number never reaches the field, so the reader never sees a letter appear and
 * then vanish. What is pasted is cleaned the same way before it lands.
 *
 * <p>A field holding several numbers is given the separator they are written with, and that stands in it too.
 *
 * @param intOnly   whether the field holds whole numbers only
 * @param separator what stands between the numbers, when the field holds more than one
 * @return the handlers to give the field
 */
export const numberOnly = (intOnly: boolean | undefined, separator?: string): {
    onKeyDown: (event: React.KeyboardEvent<HTMLInputElement>) => void
    onPaste: (event: React.ClipboardEvent<HTMLInputElement>) => void
} => {
    const stands = (character: string) =>
        character === separator || (intOnly ? WHOLE : ANY).test(character)
    return {
        onKeyDown: event => {
            // A shortcut and a key that moves or deletes are none of this; only a key that writes a character is.
            const writes = event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey
            if (writes && !stands(event.key)) {
                event.preventDefault()
            }
        },
        onPaste: event => {
            const pasted = event.clipboardData.getData('text')
            if ([...pasted].some(character => !stands(character))) {
                event.preventDefault()
            }
        },
    }
}
