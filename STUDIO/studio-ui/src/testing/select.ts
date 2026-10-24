import { fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

/**
 * Driving a select field of a form, which is found by its label.
 *
 * A field that a form draws for a watched value, such as the AWS S3 fields of a repository whose type changes, appears
 * a few tasks after the change: `Form.useWatch` reports the value in a later task. So the field is waited for.
 */

/** Opens the list of options once the field is drawn. */
export const openOptions = async (fieldLabel: string): Promise<void> => {
    fireEvent.mouseDown(await screen.findByRole('combobox', { name: fieldLabel }))
}

/** Chooses the option with the given title. */
export const chooseOption = async (fieldLabel: string, optionTitle: string): Promise<void> => {
    await openOptions(fieldLabel)
    await userEvent.click(await screen.findByTitle(optionTitle))
}
