import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

/**
 * Reading and driving the tree the input form is drawn as, for the tests of the form and of what hosts it.
 *
 * A node is addressed by its path, which is what its test ids are written with.
 */

/** Opens a node, or folds it again: a parameter starts folded, however many fields it holds. */
export const openNode = async (path: string): Promise<void> => {
    const node = screen.getByTestId(`value-${path}`).closest('.ant-tree-treenode')
    await userEvent.click(node?.querySelector('.ant-tree-switcher') as HTMLElement)
}

/** The keys of the entries of the given map, in the order the rows are drawn in. */
export const keysOf = (path: string): string[] => Array
    .from(document.querySelectorAll<HTMLInputElement>(`[data-testid^="key-${path}["]`))
    .map(key => key.value)

/** Adds an entry to the given map and names it. */
export const nameEntry = async (map: string, key: string): Promise<void> => {
    await userEvent.click(screen.getByTestId(`add-${map}`))
    await userEvent.type(screen.getByTestId(`key-${map}[]`), `${key}{enter}`)
}
