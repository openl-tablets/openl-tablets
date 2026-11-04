import { onTestFinished } from 'vitest'

/**
 * Lays out a property of an element, or of every element of a kind given its prototype, as jsdom does not: a size, a
 * scroll position, the box an element is drawn in. The property is put back as it was once the test ends.
 *
 * A list drawn only while in sight measures what it scrolls in and what it draws; jsdom lays nothing out, so a test of
 * such a list lays out what the list measures.
 */
export const layOut = (owner: object, name: string, get: (this: HTMLElement) => unknown): void => {
    const own = Object.getOwnPropertyDescriptor(owner, name)
    Object.defineProperty(owner, name, { configurable: true, get })
    onTestFinished(() => {
        if (own) {
            Object.defineProperty(owner, name, own)
        } else {
            Reflect.deleteProperty(owner, name)
        }
    })
}
