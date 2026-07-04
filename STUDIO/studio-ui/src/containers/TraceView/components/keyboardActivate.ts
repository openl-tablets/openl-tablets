import type React from 'react'

/**
 * Keyboard handler that runs the same action as a click when Enter or Space is pressed. Lets a
 * non-native interactive element (a clickable row or gutter given `role="button"` and `tabIndex={0}`)
 * be operated from the keyboard, matching the behavior of a real button.
 */
export const onActivate =
    (action: () => void) =>
        (e: React.KeyboardEvent): void => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                action()
            }
        }
