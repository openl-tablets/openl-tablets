import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { FieldRow } from './FieldRow'

vi.mock('./FieldRow.styles', () => ({
    useStyles: () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

describe('FieldRow', () => {
    it('renders a real label element for the control', () => {
        render(<FieldRow htmlFor="ctrl" label="Project name"><input data-testid="ctrl" id="ctrl" /></FieldRow>)

        // The colon comes from the style, so it is not part of the text.
        const label = screen.getByText('Project name')
        expect(label.tagName).toBe('LABEL')
        expect(label.getAttribute('for')).toBe('ctrl')
        expect(screen.getByTestId('ctrl')).toBeTruthy()
    })

    it('marks a required field on the label itself, not in its text', () => {
        // The asterisk comes from the style, so the label reads as its plain text.
        const { container } = render(<FieldRow required label="Name"><input /></FieldRow>)

        expect(screen.getByText('Name').tagName).toBe('LABEL')
        expect(container.textContent).toBe('Name')
    })

    it('points the label at the id the control brings', () => {
        render(<FieldRow label="Path"><input data-testid="ctrl" id="repo-path" /></FieldRow>)

        expect(screen.getByText('Path').getAttribute('for')).toBe('repo-path')
    })

    it('binds the label to the control it labels', () => {
        render(<FieldRow label="Comment"><textarea data-testid="ta" /></FieldRow>)

        const control = screen.getByTestId('ta')
        expect(control.getAttribute('id')).toBeTruthy()
        expect(screen.getByText('Comment').getAttribute('for')).toBe(control.getAttribute('id'))
        // A field with no name of its own is submitted under one derived from the label.
        expect(control.getAttribute('name')).toBe('comment')
    })

    it('does not bind the label to a read-only value shown as plain text', () => {
        // A row that displays a value rather than editing it passes a span; a label `for` must not point
        // at a non-form element, which the browser flags as not matching a valid field.
        render(<FieldRow label="Service Name"><span data-testid="value">svc</span></FieldRow>)

        expect(screen.getByText('Service Name').getAttribute('for')).toBeNull()
        expect(screen.getByTestId('value').getAttribute('id')).toBeNull()
    })

    it('renders an optional, top-aligned field', () => {
        render(<FieldRow alignTop label="Comment"><textarea data-testid="ta" /></FieldRow>)

        expect(screen.getByText('Comment')).toBeTruthy()
        expect(screen.getByTestId('ta')).toBeTruthy()
    })
})
