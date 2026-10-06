import { fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

/**
 * Driving a select field of a form, which is found by its label.
 */

/** Opens the list of options. */
export const openOptions = (fieldLabel: string): void => {
    fireEvent.mouseDown(screen.getByRole('combobox', { name: fieldLabel }))
}

/** Chooses the option with the given title. */
export const chooseOption = async (fieldLabel: string, optionTitle: string): Promise<void> => {
    openOptions(fieldLabel)
    await userEvent.click(await screen.findByTitle(optionTitle))
}
