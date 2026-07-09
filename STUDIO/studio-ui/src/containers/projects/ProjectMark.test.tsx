import { render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ProjectMark } from './ProjectMark'

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

const markFor = (name: string) => {
    const { container, unmount } = render(<ProjectMark name={name} />)
    const html = container.innerHTML
    unmount()
    return html
}

describe('ProjectMark', () => {
    it('renders a 2x2 grid with at least one inked cell', () => {
        const { getByTestId } = render(<ProjectMark name="Insurance-Rating" />)
        const mark = getByTestId('project-mark')
        expect(mark.children).toHaveLength(4)
        expect(mark.querySelectorAll('[data-inked]').length).toBeGreaterThan(0)
    })

    it('is deterministic: the same name always produces the same mark', () => {
        expect(markFor('Claims-Validation')).toBe(markFor('Claims-Validation'))
    })

    it('gives different names different marks', () => {
        expect(markFor('Alpha')).not.toBe(markFor('Beta'))
    })

    it('is decorative for assistive technology', () => {
        const { getByTestId } = render(<ProjectMark name="Alpha" />)
        expect(getByTestId('project-mark').getAttribute('aria-hidden')).toBe('true')
    })

    it('honors the requested size', () => {
        const { getByTestId } = render(<ProjectMark name="Alpha" size={28} />)
        expect(getByTestId('project-mark').style.width).toBe('28px')
    })
})
