import { createRef } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { markInksOf } from 'components/textMarks'
import { drawField, MarkedTextField, readField, selectIn, selectionIn } from './MarkedTextField'

const INKS = markInksOf({
    colorErrorText: '#b00000',
    colorWarningText: '#8a5a00',
    colorSuccessText: '#006400',
    colorInfoText: '#00508c',
}, ['#ffffff'])

const drawn = (value: string, marks: Parameters<typeof drawField>[2]) => {
    const field = document.createElement('div')
    document.body.append(field)
    drawField(field, value, marks, INKS)
    return field
}

describe('MarkedTextField', () => {
    it('draws a text with its marks and reads them back', () => {
        const field = drawn('Good Morning', [{ from: 5, to: 12, color: 'warning' }])

        const marked = field.querySelector<HTMLElement>('[data-mark="warning"]')
        expect(marked?.style.fontWeight).toBe('bold')
        expect(marked).toHaveStyle({ color: '#8a5a00' })
        expect(readField(field)).toEqual({ value: 'Good Morning', marks: [{ from: 5, to: 12, color: 'warning' }]})
    })

    it('reads a line break as a line of the text', () => {
        const field = drawn('one', [])
        field.append(document.createElement('br'), document.createTextNode('two'))

        expect(readField(field).value).toBe('one\ntwo')
    })

    it('reads no line from the break a browser leaves in a field it emptied', () => {
        const field = drawn('one', [{ from: 0, to: 3, color: 'info' }])
        field.querySelector('span')?.append(document.createElement('br'))

        expect(readField(field)).toEqual({ value: 'one', marks: [{ from: 0, to: 3, color: 'info' }]})
    })

    it('puts the selection over a piece of the text and reads where it stands', () => {
        const field = drawn('Good Morning', [{ from: 5, to: 12, color: 'error' }])

        selectIn(field, { from: 3, to: 7 })

        expect(selectionIn(field)).toEqual({ from: 3, to: 7 })
    })

    it('reads no selection outside the field', () => {
        const field = drawn('Good', [])
        selectIn(drawn('Other', []), { from: 0, to: 2 })

        expect(selectionIn(field)).toBeNull()
    })

    describe('written in', () => {
        const open = (multiline = false) => {
            const handlers = { onChange: vi.fn(), onCommit: vi.fn(), onCancel: vi.fn(), onSwitch: vi.fn() }
            const fieldRef = createRef<HTMLDivElement>()
            render(
                <MarkedTextField
                    fieldRef={fieldRef}
                    inks={INKS}
                    marks={[{ from: 0, to: 4, color: 'error' }]}
                    multiline={multiline}
                    selection={null}
                    value="Good Morning"
                    {...handlers}
                />
            )
            return { field: screen.getByTestId('table-cell-marked-input'), ...handlers }
        }

        it('keeps a piece written inside a mark marked', () => {
            const { field, onChange } = open()
            const marked = field.querySelector('[data-mark="error"]')
            marked!.textContent = 'Goood'
            fireEvent.input(field)

            expect(onChange).toHaveBeenCalledWith('Goood Morning', [{ from: 0, to: 5, color: 'error' }])
        })

        it('keeps what was written with Enter and leaves it with Escape', () => {
            const { field, onCommit, onCancel } = open()

            fireEvent.keyDown(field, { key: 'Enter' })
            fireEvent.keyDown(field, { key: 'Escape' })

            expect(onCommit).toHaveBeenCalledTimes(1)
            expect(onCancel).toHaveBeenCalledTimes(1)
        })

        it('asks for several lines with Alt+Enter', () => {
            const { field, onSwitch, onCommit } = open()

            fireEvent.keyDown(field, { key: 'Enter', altKey: true })

            expect(onSwitch).toHaveBeenCalled()
            expect(onCommit).not.toHaveBeenCalled()
        })

        it('writes a line of its own with Enter in a text of several lines, and keeps it with Ctrl+Enter', () => {
            const { field, onChange, onCommit } = open(true)
            selectIn(field, { from: 5, to: 5 })

            fireEvent.keyDown(field, { key: 'Enter' })
            expect(onChange).toHaveBeenLastCalledWith('Good \nMorning', [{ from: 0, to: 4, color: 'error' }])

            fireEvent.keyDown(field, { key: 'Enter', ctrlKey: true })
            expect(onCommit).toHaveBeenCalled()
        })

        it('pastes a text plain', () => {
            const { field, onChange } = open()
            selectIn(field, { from: 12, to: 12 })

            fireEvent.paste(field, { clipboardData: { getData: () => '!' } })

            expect(onChange).toHaveBeenLastCalledWith('Good Morning!', [{ from: 0, to: 4, color: 'error' }])
        })

        it('formats nothing with the keys that format a text elsewhere', () => {
            const { field } = open()
            const bold = new KeyboardEvent('keydown', { key: 'b', ctrlKey: true, bubbles: true, cancelable: true })

            field.dispatchEvent(bold)

            expect(bold.defaultPrevented).toBe(true)
        })

        it('puts the caret at the start with F2 and at the end with F3', () => {
            const { field } = open()
            selectIn(field, { from: 5, to: 5 })

            fireEvent.keyDown(field, { key: 'F2' })
            expect(selectionIn(field)).toEqual({ from: 0, to: 0 })

            fireEvent.keyDown(field, { key: 'F3' })
            expect(selectionIn(field)).toEqual({ from: 12, to: 12 })
        })

        it('keeps what was written when it loses the focus', () => {
            const { field, onCommit } = open()

            fireEvent.blur(field)

            expect(onCommit).toHaveBeenCalled()
        })
    })
})
